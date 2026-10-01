import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import adapter as a


NAME = 'int_workorder_demo001.csv'
CONTENT = (Path(__file__).resolve().parents[1] / 'examples' / NAME).read_bytes()
CONFIG = {'sourceFormat': 'oracle-work-orders-v1', 'requireReady': False,
          'stableSeconds': 60, 'ftp': {},
          'oracleWorkOrders': {'mapping': {'companyCode': '20901',
                                           'warehouseName': 'WMEC',
                                           'inventoryStatusName': 'Available'}}}


class FTP:
    def __init__(self):
        self.files = {NAME: CONTENT, 'int_item_20261001.csv': b'not a work order'}
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
        return '42', 'PENDING'
    def status(self, kind, identity):
        return self.business_status


class Guard:
    def __init__(self, existing=()):
        self.existing = set(existing)
    def check(self, records):
        return self.existing


class WorkOrderFTPTests(unittest.TestCase):
    def test_mes_precheck_blocks_missing_master_data_and_detects_existing_order(self):
        config = {'companyId': 1, 'warehouseId': 1,
                  'inventoryBaseUrl': 'http://inventory.test',
                  'workOrderBaseUrl': 'http://workorder.test'}
        guard = a.ExistingWorkOrderGuard(config)
        record = a.parse_published_file(NAME, CONTENT, CONFIG)[1][0]
        with patch.object(guard, '_list', return_value=[{'number': 'WO-TEST-001'}]):
            self.assertEqual(guard.check([record]), {'WO-TEST-001'})
        def status_and_order(url, query):
            return [] if url.endswith('/work-orders') else [{'name': 'Available'}]
        with patch.object(guard, '_list', side_effect=status_and_order), \
             patch.object(guard.inventory, 'check', return_value={'FG-001', 'RM-001'}), \
             self.assertRaisesRegex(a.InvalidFile, 'item missing'):
            guard.check([record])
        with patch.object(guard, '_list', side_effect=status_and_order), \
             patch.object(guard.inventory, 'check', return_value={'FG-001', 'RM-001', 'RM-002'}):
            self.assertEqual(guard.check([record]), set())

    def test_new_order_submitted_once_then_deleted_only_after_completion(self):
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            ftp, api = FTP(), API()
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=100):
                a.poll_once(CONFIG, state, api, Guard(), scan_all=True)
            self.assertEqual(api.calls, [])
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=161):
                a.poll_once(CONFIG, state, api, Guard(), scan_all=True)
            self.assertEqual(len(api.calls), 1)
            self.assertEqual(api.calls[0][0], 'work-orders')
            self.assertIn(NAME, ftp.files)
            api.business_status = 'COMPLETED'
            with patch.object(a, 'ftp_connect', return_value=ftp):
                a.poll_once(CONFIG, state, api, Guard(), scan_all=True)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual(report['records'][0]['state'], 'COMPLETED')
            self.assertEqual(report['sourceCleanup'], 'DELETED')
            self.assertNotIn(NAME, ftp.files)
            self.assertIn('int_item_20261001.csv', ftp.files)

    def test_existing_order_skipped_without_put(self):
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            ftp, api = FTP(), API()
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=100):
                a.poll_once(CONFIG, state, api, Guard(), scan_all=True)
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=161):
                a.poll_once(CONFIG, state, api, Guard({'WO-TEST-001'}), scan_all=True)
            report = json.loads((state / (NAME + '.report.json')).read_text())
            self.assertEqual(report['records'][0]['state'], 'SKIPPED_EXISTING')
            self.assertEqual(report['records'][0]['workOrderNumber'], 'WO-TEST-001')
            self.assertEqual(report['records'][0]['skipReason'],
                             'Work Order number already exists in MES')
            self.assertEqual(api.calls, [])
            self.assertEqual(report['sourceCleanup'], 'DELETED')

    def test_failed_precheck_does_not_submit_or_delete(self):
        class FailedGuard:
            def check(self, records):
                raise a.InvalidFile('missing component')
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            ftp, api = FTP(), API()
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=100):
                a.poll_once(CONFIG, state, api, Guard(), scan_all=True)
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=161):
                a.poll_once(CONFIG, state, api, FailedGuard(), scan_all=True)
            self.assertEqual(api.calls, [])
            self.assertIn(NAME, ftp.files)
            self.assertEqual(json.loads((state / (NAME + '.report.json')).read_text())['state'], 'REJECTED')

    def test_second_batch_cannot_submit_order_before_first_is_completed(self):
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp)
            ftp, api = FTP(), API()
            second = 'int_workorder_demo002.csv'
            ftp.files[second] = CONTENT
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=100):
                a.poll_once(CONFIG, state, api, Guard(), scan_all=True)
            with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=161):
                a.poll_once(CONFIG, state, api, Guard(), scan_all=True)
            self.assertEqual(len(api.calls), 1)
            self.assertEqual(json.loads((state / (second + '.report.json')).read_text())['state'],
                             'REJECTED')
            self.assertIn(second, ftp.files)


if __name__ == '__main__':
    unittest.main()
