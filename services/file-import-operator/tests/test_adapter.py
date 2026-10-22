import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import adapter as a

EXAMPLES = Path(__file__).resolve().parents[1] / 'examples'
NAME = 'suppliers__demo001.csv'
CONTENT = (EXAMPLES / NAME).read_bytes()


class API:
    def __init__(self):
        self.calls = []
        self.business_status = 'PENDING'
        self.fail = False

    def submit(self, kind, payload):
        self.calls.append((kind, payload))
        if self.fail:
            raise TimeoutError()
        return str(len(self.calls)), 'PENDING'

    def status(self, kind, identity):
        return self.business_status


class ParsingTests(unittest.TestCase):
    def test_six_csv_xml_pairs_match(self):
        for kind in a.SCHEMAS:
            with self.subTest(kind=kind):
                stem = kind + '__demo001'
                csv = a.parse_file(stem + '.csv', (EXAMPLES / (stem + '.csv')).read_bytes())
                xml = a.parse_file(stem + '.xml', (EXAMPLES / (stem + '.xml')).read_bytes())
                self.assertEqual(csv, xml)
                self.assertEqual(len(csv[1]), 1)

    def test_work_order_initializes_workflow_and_lines(self):
        name = 'work-orders__demo001.csv'
        record = a.parse_file(name, (EXAMPLES / name).read_bytes())[1][0]
        self.assertEqual(record['payload']['status'], 'PENDING')
        self.assertEqual(len(record['payload']['workOrderLines']), 2)
        for line in record['payload']['workOrderLines']:
            self.assertEqual(line['status'], 'ATTACHED')
            self.assertEqual(line['warehouseName'], 'TEST_WAREHOUSE')

    def test_bom_quoted_multiline_csv(self):
        content = '\ufeffrecordId,companyCode,warehouseName,name,description\r\ne1,C,W,S,"hello,\nworld"\r\n'.encode()
        self.assertEqual(a.parse_file(NAME, content)[1][0]['payload']['description'], 'hello,\nworld')

    def test_invalid_files(self):
        bad = [
            b'recordId,companyCode,warehouseName,name\na,C,W\n',
            b'recordId,companyCode,warehouseName,name\na,C,W,S,extra\n',
            b'recordId,companyCode,warehouseName,name,name\na,C,W,S,S\n',
            b'recordId,companyCode,warehouseName,name,sql\na,C,W,S,select\n',
            CONTENT + CONTENT.splitlines(keepends=True)[1],
            b'\xff', b'', b'x' * (a.MAX_BYTES + 1),
        ]
        for content in bad:
            with self.subTest(content=content[:60]), self.assertRaises(a.InvalidFile):
                a.parse_file(NAME, content)
        with self.assertRaises(a.InvalidFile):
            a.parse_file('../' + NAME, CONTENT)

    def test_xml_rejects_entities_attributes_and_unexpected_text(self):
        for text in ['<!DOCTYPE batch [<!ENTITY x "X">]><batch/>',
                     '<batch attr="x"/>', '<batch><record/>bad</batch>',
                     '<batch><record><name><nested/></name></record></batch>']:
            with self.assertRaises(a.InvalidFile):
                a.parse_file('suppliers__a.xml', text.encode())

    def test_numeric_limits_and_booleans(self):
        for field, value in [('quantity', '2147483648'), ('expectedQuantity', '1.5'),
                             ('expectedQuantity', '-1'), ('unitCost', 'NaN'),
                             ('unitCost', 'Infinity'), ('nonInventoryItem', 'yes')]:
            with self.subTest(field=field), self.assertRaises(a.InvalidFile):
                a.typed(field, value)

    def test_header_conflicts_and_duplicate_lines(self):
        name = 'orders__demo001.csv'
        raw = (EXAMPLES / name).read_bytes()
        for bad in [raw.replace(b'SO-001', b'SO-OTHER', 1),
                    raw.replace(b',2,ITEM-002', b',1,ITEM-002')]:
            with self.assertRaises(a.InvalidFile):
                a.parse_file(name, bad)


class LedgerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / 'ledger.db'
        self.ledger = a.Ledger(self.path)
        self.records = a.parse_file(NAME, CONTENT)[1]
        self.api = API()

    def tearDown(self):
        self.ledger.db.close()
        self.temp.cleanup()

    def state(self):
        return self.ledger.get('suppliers', 'suppliers-event-001')['state']

    def test_dedup_and_business_completion(self):
        a.submit_records(self.records, self.ledger, self.api)
        a.submit_records(self.records, self.ledger, self.api)
        self.assertEqual(len(self.api.calls), 1)
        self.assertEqual(self.state(), 'ACCEPTED')
        a.refresh_status(self.ledger, self.api)
        self.assertEqual(self.state(), 'ACCEPTED')
        self.api.business_status = 'COMPLETED'
        a.refresh_status(self.ledger, self.api)
        self.assertEqual(self.state(), 'COMPLETED')

    def test_business_error_is_terminal_without_replay(self):
        a.submit_records(self.records, self.ledger, self.api)
        self.api.business_status = 'ERROR'
        a.refresh_status(self.ledger, self.api)
        a.submit_records(self.records, self.ledger, self.api)
        self.assertEqual(self.state(), 'BUSINESS_ERROR')
        self.assertEqual(len(self.api.calls), 1)

    def test_timeout_is_not_replayed_after_restart(self):
        self.api.fail = True
        a.submit_records(self.records, self.ledger, self.api)
        self.assertEqual(self.state(), 'UNCERTAIN')
        self.ledger.db.close()
        self.ledger = a.Ledger(self.path)
        self.api.fail = False
        a.submit_records(self.records, self.ledger, self.api)
        self.assertEqual(len(self.api.calls), 1)

    def test_crash_during_send_is_uncertain(self):
        self.ledger.prepare(self.records)
        self.ledger.set('suppliers', 'suppliers-event-001', 'SENDING')
        self.ledger.db.close()
        self.ledger = a.Ledger(self.path)
        a.submit_records(self.records, self.ledger, self.api)
        self.assertEqual(self.state(), 'UNCERTAIN')
        self.assertEqual(self.api.calls, [])

    def test_conflicting_record_rolls_back_entire_preflight(self):
        self.ledger.prepare(self.records)
        new = {**self.records[0], 'recordId': 'new'}
        changed = {**self.records[0], 'payload': {'name': 'changed'}}
        with self.assertRaises(a.InvalidFile):
            a.submit_records([new, changed], self.ledger, self.api)
        self.assertIsNone(self.ledger.get('suppliers', 'new'))
        self.assertEqual(self.api.calls, [])


class ProtocolTests(unittest.TestCase):
    def test_work_order_status_uses_list_endpoint_with_company_code(self):
        api = a.MesAPI('https://example.invalid/api/integration', company_code='20901')
        with patch.object(api, 'request', return_value=[{'id': 43345, 'status': 'COMPLETED'}]) as request:
            self.assertEqual(api.status('work-orders', '43345'), 'COMPLETED')
        request.assert_called_once_with('GET',
                                        '/integration-data/work-orders?companyCode=20901&id=43345')

    def test_api_unwrap_and_method(self):
        api = a.MesAPI('https://example.invalid/api/integration', 'TEST_TOKEN')
        class Opener:
            def open(self, request, timeout):
                self.request = request
                return io.BytesIO(b'{"result":0,"data":{"id":12,"status":"PENDING"}}')
        api.opener = opener = Opener()
        self.assertEqual(api.submit('items', {'name': 'X'}), ('12', 'PENDING'))
        self.assertEqual(opener.request.method, 'PUT')
        self.assertTrue(opener.request.full_url.endswith('/integration-data/items'))
        self.assertEqual(json.loads(opener.request.data), {'name': 'X'})
        self.assertEqual(api.status('items', '12'), 'PENDING')
        self.assertEqual(opener.request.method, 'GET')
        with self.assertRaises(ValueError):
            api.status('items', '13')

    def test_http_200_error_envelopes_are_rejected(self):
        api = a.MesAPI('https://example.invalid')
        for body in [b'{"result":1,"data":12}', b'{"result":false,"data":12}', b'{"id":12}']:
            with patch.object(api.opener, 'open', return_value=io.BytesIO(body)):
                with self.assertRaises(ValueError):
                    api.submit('items', {})

    def test_ready_marker_dedup_and_changed_file_report(self):
        api = API()
        class FTP:
            files = {NAME: CONTENT}
            def nlst(self): return list(self.files)
            def retrbinary(self, command, callback): callback(self.files[command[5:]])
            def close(self): pass
        ftp = FTP()
        with tempfile.TemporaryDirectory() as temp, patch.object(a, 'ftp_connect', return_value=ftp):
            directory = Path(temp)
            a.poll_once({'ftp': {}}, directory, api)
            self.assertEqual(api.calls, [])
            ftp.files[NAME + '.ready'] = b''
            a.poll_once({'ftp': {}}, directory, api)
            a.poll_once({'ftp': {}}, directory, api)
            self.assertEqual(len(api.calls), 1)
            ftp.files[NAME] = CONTENT.replace(b'Test supplier', b'Changed supplier')
            a.poll_once({'ftp': {}}, directory, api)
            report = json.loads((directory / (NAME + '.report.json')).read_text())
            self.assertEqual(report['state'], 'REJECTED')
            self.assertEqual(len(api.calls), 1)
            self.assertEqual((directory / NAME).read_bytes(), CONTENT)


if __name__ == '__main__':
    unittest.main()
