import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import adapter as a

EXAMPLES = Path(__file__).resolve().parents[1] / 'examples'
CONTENT = (EXAMPLES / 'oracle-items-demo.csv').read_bytes()
OPTIONS = json.loads((EXAMPLES.parent / 'item-mapping.example.json').read_text())
MEASUREMENTS = {role: {'length': 1, 'width': 1, 'height': 1, 'weight': 1}
                for role in ('piece', 'carton', 'pallet')}
CONFIG = {'sourceFormat': 'oracle-items-v1', 'ftp': {},
          'oracleItems': {'mapping': {**OPTIONS, 'unitMeasurements': MEASUREMENTS}}}
NAME = 'int_item__demo001.csv'


class FTP:
    def __init__(self):
        self.files = {NAME: CONTENT,
                      'UNSHIP_INV_20260930.csv': b'not an item',
                      'int_order__demo001.csv': b'not an item',
                      'int_order__demo001.csv.ready': b''}

    def nlst(self):
        return list(self.files)

    def retrbinary(self, command, callback):
        callback(self.files[command[5:]])

    def delete(self, name):
        del self.files[name]

    def close(self):
        pass


class API:
    def __init__(self):
        self.calls = []
        self.business_status = 'PENDING'

    def submit(self, kind, payload):
        self.calls.append((kind, payload))
        return str(len(self.calls)), 'PENDING'

    def status(self, kind, identity):
        return self.business_status


class Guard:
    def __init__(self):
        self.checked = []
        self.existing = set()

    def check(self, records):
        self.checked.extend(r['payload']['name'] for r in records)
        for record in records:
            if record['payload']['name'] in self.existing:
                raise a.InvalidFile('existing MES item: ' + record['payload']['name'])


class OracleFtpTests(unittest.TestCase):
    def test_ready_poll_dedup_status_and_cross_batch_guard(self):
        ftp, api, guard = FTP(), API(), Guard()
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(api.calls, [])
            ftp.files[NAME + '.ready'] = b''
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(len(api.calls), 3)
            self.assertEqual(guard.checked, ['TEST-ITEM-001', 'TEST-ITEM-002', 'TEST-ITEM-003'])
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(len(api.calls), 3)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']], ['ACCEPTED'] * 3)
            self.assertEqual(report['sourceCleanup'], 'PENDING')
            self.assertIn(NAME, ftp.files)
            self.assertEqual([len(r['defaultsApplied']) for r in report['records']], [0, 2, 2])
            api.business_status = 'COMPLETED'
            a.poll_once(CONFIG, state, api, guard)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']], ['COMPLETED'] * 3)
            self.assertEqual(report['sourceCleanup'], 'DELETED')
            self.assertNotIn(NAME, ftp.files)
            self.assertNotIn(NAME + '.ready', ftp.files)
            self.assertIn('UNSHIP_INV_20260930.csv', ftp.files)
            alternate = 'int_item__demo002.csv'
            ftp.files[alternate] = CONTENT
            ftp.files[alternate + '.ready'] = b''
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(len(api.calls), 3)
            rejected = json.loads((state / (alternate + '.report.json')).read_text())
            self.assertEqual(rejected['state'], 'REJECTED')
            self.assertIn('another processed batch', rejected['reason'])

    def test_existing_item_rejects_entire_file_before_submit(self):
        ftp, api, guard = FTP(), API(), Guard()
        guard.existing.add('TEST-ITEM-002')
        ftp.files[NAME + '.ready'] = b''
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(api.calls, [])
            self.assertFalse((state / NAME).exists())
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual(report['state'], 'REJECTED')
            self.assertIn('TEST-ITEM-002', report['reason'])

    def test_changed_published_file_rejected(self):
        ftp, api, guard = FTP(), API(), Guard()
        ftp.files[NAME + '.ready'] = b''
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            a.poll_once(CONFIG, state, api, guard)
            ftp.files[NAME] = CONTENT.replace(b'Test item normal', b'Test item changed')
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(len(api.calls), 3)
            self.assertEqual(json.loads((state / (NAME + '.report.json')).read_text())['state'], 'REJECTED')

    def test_ftp_failure_refreshes_existing_report_but_fails_scan(self):
        ftp, api, guard = FTP(), API(), Guard()
        ftp.files[NAME + '.ready'] = b''
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            with patch.object(a, 'ftp_connect', return_value=ftp):
                a.poll_once(CONFIG, state, api, guard)
            api.business_status = 'COMPLETED'
            with patch.object(a, 'ftp_connect', side_effect=ConnectionError()), self.assertRaises(ConnectionError):
                a.poll_once(CONFIG, state, api, guard)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']], ['COMPLETED'] * 3)
            self.assertEqual(report['sourceCleanup'], 'PENDING')

    def test_business_error_keeps_source_and_ready(self):
        ftp, api, guard = FTP(), API(), Guard()
        ftp.files[NAME + '.ready'] = b''
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            a.poll_once(CONFIG, state, api, guard)
            api.business_status = 'ERROR'
            a.poll_once(CONFIG, state, api, guard)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']], ['BUSINESS_ERROR'] * 3)
            self.assertEqual(report['sourceCleanup'], 'PENDING')
            self.assertIn(NAME, ftp.files)
            self.assertIn(NAME + '.ready', ftp.files)

    def test_marker_only_cleanup_retry_after_partial_delete(self):
        ftp, api, guard = FTP(), API(), Guard()
        ftp.files[NAME + '.ready'] = b''
        original_delete = ftp.delete
        failed = False
        def delete(name):
            nonlocal failed
            if name.endswith('.ready') and not failed:
                failed = True
                raise ConnectionError()
            original_delete(name)
        ftp.delete = delete
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            a.poll_once(CONFIG, state, api, guard)
            api.business_status = 'COMPLETED'
            a.poll_once(CONFIG, state, api, guard)
            self.assertNotIn(NAME, ftp.files)
            self.assertIn(NAME + '.ready', ftp.files)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual(report['sourceCleanup'], 'DELETE_FAILED')
            a.poll_once(CONFIG, state, api, guard)
            self.assertNotIn(NAME + '.ready', ftp.files)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual(report['sourceCleanup'], 'DELETED')

    def test_batch_filename_variants(self):
        for name in ('int_item20260930.csv', 'int_item_20260930.csv',
                     'int_item__20260930.csv', 'int_item-20260930.csv'):
            with self.subTest(name=name):
                self.assertTrue(a.published_file(name, CONFIG))
        for name in ('int_item.csv', 'int_order_20260930.csv',
                     'UNSHIP_INV_20260930.csv', 'int_item_20260930.xml'):
            with self.subTest(name=name):
                self.assertFalse(a.published_file(name, CONFIG))

    def test_inventory_guard_checks_server_response_and_exact_name(self):
        guard = a.ExistingItemGuard({'inventoryBaseUrl': 'http://example.invalid',
                                     'companyId': 20901, 'warehouseId': 1})
        records = a.parse_published_file(NAME, CONTENT, CONFIG)[1]
        class Opener:
            def __init__(self, body):
                self.body = body
            def open(self, request, timeout):
                self.request = request
                return io.BytesIO(self.body)
        guard.opener = opener = Opener(b'{"result":0,"data":[{"name":"OTHER"}]}')
        guard.check(records[:1])
        self.assertIn('companyId=20901', opener.request.full_url)
        guard.opener = Opener(b'{"result":0,"data":[{"name":"TEST-ITEM-001"}]}')
        with self.assertRaises(a.InvalidFile):
            guard.check(records[:1])
        guard.opener = Opener(b'{"result":1,"data":[]}')
        with self.assertRaises(a.InvalidFile):
            guard.check(records[:1])


if __name__ == '__main__':
    unittest.main()
