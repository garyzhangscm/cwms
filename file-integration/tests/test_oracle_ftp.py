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
NO_READY_CONFIG = {**CONFIG, 'requireReady': False, 'stableSeconds': 60}
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
        return {record['payload']['name'] for record in records
                if record['payload']['name'] in self.existing}


class OracleFtpTests(unittest.TestCase):
    def test_script_entrypoint_accepts_header_only_item_file(self):
        # adapter.py executed as __main__ and oracle_items imported as adapter
        # used to produce distinct InvalidFile classes and abort the scan.
        source = Path(a.__file__).read_text().split("if __name__ == '__main__':")[0]
        script = {'__name__': '__main__', '__file__': a.__file__}
        exec(compile(source, a.__file__, 'exec'), script)
        empty = b'segment1,description,item_type,pieces_per_carton,pieces_per_pallet\n'
        self.assertEqual(script['parse_published_file'](NAME, empty, CONFIG)[1], [])

    def test_header_only_item_file_is_recorded_and_deleted(self):
        ftp, api, guard = FTP(), API(), Guard()
        ftp.files[NAME] = b'segment1,description,item_type,pieces_per_carton,pieces_per_pallet\n'
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            config = {**NO_READY_CONFIG, 'stableSeconds': 0}
            a.poll_once(config, state, api, guard, scan_all=True)
            a.poll_once(config, state, api, guard, scan_all=True)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual(report['state'], 'NO_NEW_ITEMS')
            self.assertEqual(report['records'], [])
            self.assertEqual(report['sourceCleanup'], 'DELETED')
            self.assertEqual(api.calls, [])
            self.assertNotIn(NAME, ftp.files)
            self.assertEqual((state / NAME).read_bytes(),
                             b'segment1,description,item_type,pieces_per_carton,pieces_per_pallet\n')

    def test_live_item_type_mapping_is_reloaded_for_each_scan(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'mapping.json'
            path.write_text('{"01":"Finish Good"}')
            config = {**NO_READY_CONFIG,
                      'oracleItems': {**NO_READY_CONFIG['oracleItems'],
                                      'itemTypeMappingFile': str(path)}}
            self.assertEqual(a.effective_config(config)['oracleItems']['mapping']['itemTypeMapping'],
                             {'01': 'Finish Good'})
            path.write_text('{"01":"Finish Good","02":"Raw Material"}')
            self.assertEqual(a.effective_config(config)['oracleItems']['mapping']['itemTypeMapping'],
                             {'01': 'Finish Good', '02': 'Raw Material'})
            self.assertEqual(config['oracleItems']['mapping']['itemTypeMapping'],
                             {'01': 'Finish Good'})

    def test_ftp_credential_files_are_read_without_environment_variables(self):
        class LoginFTP:
            def connect(self, host, port, timeout):
                pass
            def login(self, username, password):
                self.credentials = (username, password)
            def cwd(self, inbox):
                pass
        with tempfile.TemporaryDirectory() as temp:
            user, password = Path(temp) / 'user', Path(temp) / 'password'
            user.write_text('operator\n')
            password.write_text('secret with spaces\n')
            instance = LoginFTP()
            with patch.object(a.ftplib, 'FTP', return_value=instance):
                result = a.ftp_connect({'host': 'ftp.example', 'inbox': '/WIS',
                                        'usernameFile': str(user), 'passwordFile': str(password)})
            self.assertIs(result, instance)
            self.assertEqual(instance.credentials, ('operator', 'secret with spaces'))

    def test_csv_only_scan_all_handles_multiple_batches_without_ready_files(self):
        ftp, api, guard = FTP(), API(), Guard()
        other = 'int_item__demo002.csv'
        ftp.files[other] = CONTENT.replace(b'TEST-ITEM', b'OTHER-ITEM')
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            with patch.object(a.time, 'time', return_value=100):
                a.poll_once(NO_READY_CONFIG, state, api, guard, scan_all=True)
            self.assertEqual(api.calls, [])
            with patch.object(a.time, 'time', return_value=160):
                a.poll_once(NO_READY_CONFIG, state, api, guard, scan_all=True)
            self.assertEqual(len(api.calls), 6)
            self.assertFalse((state / 'int_order__demo001.csv').exists())
            api.business_status = 'COMPLETED'
            with patch.object(a.time, 'time', return_value=161):
                a.poll_once(NO_READY_CONFIG, state, api, guard, scan_all=True)
            self.assertNotIn(NAME, ftp.files)
            self.assertNotIn(other, ftp.files)
            self.assertIn('UNSHIP_INV_20260930.csv', ftp.files)

    def test_csv_only_mode_requires_target_and_stable_content(self):
        ftp, api, guard = FTP(), API(), Guard()
        other = 'int_item__other.csv'
        ftp.files[other] = CONTENT.replace(b'TEST-ITEM', b'OTHER-ITEM')
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            with self.assertRaises(ValueError):
                a.poll_once(NO_READY_CONFIG, state, api, guard)
            with patch.object(a.time, 'time', return_value=100):
                a.poll_once(NO_READY_CONFIG, state, api, guard, NAME)
            self.assertEqual(api.calls, [])
            self.assertEqual(json.loads((state / (NAME + '.report.json')).read_text())['state'], 'WAITING_STABLE')
            with patch.object(a.time, 'time', return_value=159):
                a.poll_once(NO_READY_CONFIG, state, api, guard, NAME)
            self.assertEqual(api.calls, [])
            with patch.object(a.time, 'time', return_value=160):
                a.poll_once(NO_READY_CONFIG, state, api, guard, NAME)
            self.assertEqual(len(api.calls), 3)
            self.assertFalse((state / other).exists())
            self.assertIn(NAME, ftp.files)
            api.business_status = 'COMPLETED'
            with patch.object(a.time, 'time', return_value=161):
                a.poll_once(NO_READY_CONFIG, state, api, guard, NAME)
            self.assertNotIn(NAME, ftp.files)
            self.assertIn(other, ftp.files)
            self.assertEqual(json.loads((state / (NAME + '.report.json')).read_text())['sourceCleanup'], 'DELETED')

    def test_csv_only_mode_resets_stability_after_content_change(self):
        ftp, api, guard = FTP(), API(), Guard()
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            with patch.object(a.time, 'time', return_value=100):
                a.poll_once(NO_READY_CONFIG, state, api, guard, NAME)
            ftp.files[NAME] = CONTENT.replace(b'Test item normal', b'Test item changed')
            with patch.object(a.time, 'time', return_value=160):
                a.poll_once(NO_READY_CONFIG, state, api, guard, NAME)
            self.assertEqual(api.calls, [])
            with patch.object(a.time, 'time', return_value=220):
                a.poll_once(NO_READY_CONFIG, state, api, guard, NAME)
            self.assertEqual(len(api.calls), 3)

    def test_oracle_mode_requires_existing_item_guard(self):
        with tempfile.TemporaryDirectory() as temp, self.assertRaises(ValueError):
            a.poll_once(CONFIG, Path(temp), API())

    def test_failed_existing_item_lookup_blocks_submission(self):
        ftp, api = FTP(), API()
        ftp.files[NAME + '.ready'] = b''
        class FailedGuard:
            def check(self, records):
                raise ConnectionError()
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            with self.assertRaises(ConnectionError):
                a.poll_once(CONFIG, Path(temp), api, FailedGuard())
            self.assertEqual(api.calls, [])
            self.assertIn(NAME, ftp.files)

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

    def test_existing_item_is_skipped_and_new_items_complete(self):
        ftp, api, guard = FTP(), API(), Guard()
        guard.existing.add('TEST-ITEM-002')
        ftp.files[NAME + '.ready'] = b''
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(len(api.calls), 2)
            self.assertEqual([payload['name'] for _, payload in api.calls],
                             ['TEST-ITEM-001', 'TEST-ITEM-003'])
            self.assertTrue((state / NAME).exists())
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']],
                             ['ACCEPTED', 'SKIPPED_EXISTING', 'ACCEPTED'])
            self.assertEqual(report['sourceCleanup'], 'PENDING')
            api.business_status = 'COMPLETED'
            a.poll_once(CONFIG, state, api, guard)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']],
                             ['COMPLETED', 'SKIPPED_EXISTING', 'COMPLETED'])
            self.assertEqual(report['sourceCleanup'], 'DELETED')
            self.assertNotIn(NAME, ftp.files)

    def test_all_existing_items_are_skipped_and_source_deleted(self):
        ftp, api, guard = FTP(), API(), Guard()
        guard.existing = {'TEST-ITEM-001', 'TEST-ITEM-002', 'TEST-ITEM-003'}
        ftp.files[NAME + '.ready'] = b''
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            state = Path(temp)
            a.poll_once(CONFIG, state, api, guard)
            self.assertEqual(api.calls, [])
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']], ['SKIPPED_EXISTING'] * 3)
            self.assertEqual(report['sourceCleanup'], 'DELETED')
            self.assertNotIn(NAME, ftp.files)
            self.assertNotIn(NAME + '.ready', ftp.files)

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
        self.assertEqual(guard.check(records[:1]), set())
        self.assertIn('companyId=20901', opener.request.full_url)
        special = [dict(records[0], payload=dict(records[0]['payload'], name='ITEM%+ 1'))]
        guard.check(special)
        self.assertIn('name=ITEM%2525%252B%25201', opener.request.full_url)
        guard.opener = Opener(b'{"result":0,"data":[{"name":"TEST-ITEM-001"}]}')
        self.assertEqual(guard.check(records[:1]), {'TEST-ITEM-001'})
        guard.opener = Opener(b'{"result":1,"data":[]}')
        with self.assertRaises(a.InvalidFile):
            guard.check(records[:1])


if __name__ == '__main__':
    unittest.main()
