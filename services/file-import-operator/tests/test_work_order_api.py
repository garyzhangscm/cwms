import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import adapter as a
import work_order_api as web
from test_oracle_work_order_ftp import CONTENT, CONFIG, API, FTP, NAME


class Guard:
    warehouse_id = 1
    def inspect(self, records):
        return {r['recordId']: {'existing': False, 'error': 'Missing RM-001' if r['workOrderNumber'] == 'BAD' else '',
                                'missingItems': ['RM-001'] if r['workOrderNumber'] == 'BAD' else []} for r in records}


class ImportOperatorTests(unittest.TestCase):
    def records(self):
        record = a.parse_published_file('int_workorder_demo.csv', CONTENT, CONFIG)[1][0]
        other = json.loads(json.dumps(record))
        other['recordId'] = 'other'; other['workOrderNumber'] = 'BAD'; other['payload']['number'] = 'BAD'
        return [record, other]

    def test_invalid_order_does_not_block_valid_order_and_repeat_does_not_duplicate(self):
        with tempfile.TemporaryDirectory() as temp:
            ledger = a.Ledger(Path(temp) / 'ledger.sqlite3')
            api = API(); records = self.records()
            for _ in range(2):
                a.process_work_order_records(records, ledger, api, Guard())
            self.assertEqual(len(api.calls), 1)
            self.assertEqual(ledger.get('work-orders', 'other')['state'], 'BLOCKED')
            self.assertEqual(records[1]['missingItems'], ['RM-001'])
            ledger.db.close()

    def test_check_only_performs_no_submit_or_claim(self):
        with tempfile.TemporaryDirectory() as temp:
            ledger = a.Ledger(Path(temp) / 'ledger.sqlite3'); api = API()
            a.process_work_order_records(self.records(), ledger, api, Guard(), check_only=True)
            self.assertEqual(api.calls, [])
            self.assertEqual(ledger.db.execute('SELECT count(*) FROM work_order_claims').fetchone()[0], 0)
            ledger.db.close()

    def test_partial_batch_report_and_targeted_retry_leave_other_orders_untouched(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp); ftp = FTP(); api = API()
            ftp.files[NAME] = CONTENT + b'\n'.join(CONTENT.replace(b'WO-TEST-001', b'BAD').splitlines()[1:]) + b'\n'
            config = dict(CONFIG, workOrderExceptionHandling=True)
            for now in (100, 161):
                with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=now):
                    a.poll_once(config, path, api, Guard(), target_file=NAME, check_only=True)
            self.assertEqual(api.calls, [])
            with patch.object(a, 'ftp_connect', return_value=ftp):
                a.poll_once(config, path, api, Guard(), target_file=NAME, target_work_order='WO-TEST-001')
            self.assertEqual(len(api.calls), 1)
            report = json.loads((path / (NAME + '.report.json')).read_text())
            bad = next(r for r in report['records'] if r['workOrderNumber'] == 'BAD')
            self.assertEqual(bad['state'], 'BLOCKED')
            self.assertEqual(bad['missingItems'], ['RM-001'])
            self.assertIn(NAME, ftp.files)

    def test_real_guard_isolates_missing_items_and_caches_shared_lookups(self):
        config = {'companyId': 1, 'warehouseId': 1, 'inventoryBaseUrl': 'http://inventory.test',
                  'workOrderBaseUrl': 'http://work.test'}
        guard = a.ExistingWorkOrderGuard(config)
        records = self.records()
        records[0]['payload']['workOrderLines'][0]['itemName'] = 'OK'
        def inventory(items, allow_casefold=False):
            return {r['payload']['name'] for r in items if r['payload']['name'] != 'RM-001'}
        with patch.object(guard, '_list', side_effect=lambda url, query: [] if url.endswith('/work-orders') else [{'name': 'AVAL'}]), \
             patch.object(guard.inventory, 'check', side_effect=inventory) as query:
            result = guard.inspect(records)
        self.assertEqual(result[records[0]['recordId']]['error'], '')
        self.assertEqual(result['other']['missingItems'], ['RM-001'])
        queried = [r['payload']['name'] for call in query.call_args_list for r in call[0][0]]
        self.assertEqual(queried.count('FG-001'), 1)

    def test_malformed_batch_keeps_original_and_does_not_stop_other_batch(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp); ftp = FTP(); api = API()
            bad = 'int_workorder_bad.csv'; ftp.files[bad] = b'WRONG_HEADER\n'
            for now in (100, 161):
                with patch.object(a, 'ftp_connect', return_value=ftp), patch.object(a.time, 'time', return_value=now):
                    a.poll_once(dict(CONFIG, workOrderExceptionHandling=True), path, api, Guard(), scan_all=True)
            self.assertEqual((path / bad).read_bytes(), b'WRONG_HEADER\n')
            self.assertEqual(len(api.calls), 1)
            self.assertIn(bad, ftp.files)
            self.assertEqual(json.loads((path / (bad + '.report.json')).read_text())['state'], 'REJECTED')

    def test_unknown_result_never_blindly_resubmits(self):
        with tempfile.TemporaryDirectory() as temp:
            ledger = a.Ledger(Path(temp) / 'ledger.sqlite3'); api = API()
            records = self.records()[:1]; ledger.prepare(records)
            ledger.set('work-orders', records[0]['recordId'], 'UNCERTAIN')
            api.recover_work_order = lambda *args: None
            a.process_work_order_records(records, ledger, api, Guard(), retry_failed=True)
            self.assertEqual(api.calls, [])
            self.assertEqual(ledger.get('work-orders', records[0]['recordId'])['state'], 'UNCERTAIN')
            ledger.db.close()

    def test_business_error_retries_verified_existing_id_not_new_put(self):
        with tempfile.TemporaryDirectory() as temp:
            ledger = a.Ledger(Path(temp) / 'ledger.sqlite3'); api = API()
            records = self.records()[:1]; ledger.prepare(records)
            ledger.set('work-orders', records[0]['recordId'], 'BUSINESS_ERROR', '42', 'ERROR')
            api.recover_work_order = lambda *args: {'id': 42, 'status': 'ERROR'}
            sent = []; api.resend_work_order = sent.append
            a.process_work_order_records(records, ledger, api, Guard(), retry_failed=True)
            self.assertEqual(api.calls, []); self.assertEqual(sent, ['42'])
            ledger.db.close()

    def config(self, path):
        return dict(CONFIG, stateDirectory=str(path), mes={'baseUrl': 'http://mes.test'},
                    oracleWorkOrders=dict(CONFIG['oracleWorkOrders'], companyId=1, warehouseId=1,
                                          inventoryBaseUrl='http://inventory.test', workOrderBaseUrl='http://work.test'),
                    operatorApi={'authBaseUrl': 'http://auth.test', 'userBaseUrl': 'http://user.test',
                                 'layoutBaseUrl': 'http://layout.test', 'bind': '127.0.0.1', 'port': 18792})

    def store(self, path):
        (path / 'int_workorder_demo.csv').write_bytes(CONTENT)
        (path / 'int_workorder_demo.csv.report.json').write_text(json.dumps({'file': 'int_workorder_demo.csv', 'records': []}))
        return web.WorkOrderStore(self.config(path))

    def test_selection_validation_duplicate_actions_and_actor_audit(self):
        with tempfile.TemporaryDirectory() as temp:
            store = self.store(Path(temp)); user = {'username': 'alice'}
            payload = {'file': 'int_workorder_demo.csv', 'action': 'check', 'workOrderNumber': 'WO-TEST-001'}
            job = store.enqueue(payload, user)
            self.assertEqual(job['actor'], 'alice')
            with self.assertRaises(web.SettingsError): store.enqueue(payload, user)
            with self.assertRaises(web.SettingsError): store.enqueue(dict(payload, file='../config.json'), user)
            with self.assertRaises(web.SettingsError): store.enqueue(dict(payload, workOrderNumber='OTHER'), user)
            with self.assertRaises(web.SettingsError): store.enqueue(dict(payload, command='rm'), user)
            self.assertEqual(store.job_list()[0]['id'], job['id'])

    def test_job_waits_for_daily_lock_and_check_does_not_send(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp); store = self.store(path)
            job = store.enqueue({'file': 'int_workorder_demo.csv', 'action': 'check'}, {'username': 'alice'})
            with a.state_lock(path):
                self.assertFalse(store.run_next())
                self.assertEqual(store.job_list()[0]['state'], 'QUEUED')
            with patch.object(a, 'poll_once') as poll:
                self.assertTrue(store.run_next())
                self.assertTrue(poll.call_args[1]['check_only'])
                self.assertEqual(store.job_list()[0]['state'], 'FINISHED')

    def test_company_warehouse_scope_and_retry_permission_are_enforced(self):
        with tempfile.TemporaryDirectory() as temp:
            store = self.store(Path(temp))
            with self.assertRaises(web.SettingsError): store.authorize({'warehouseId': '2'})
            user = {'username': 'alice', 'roles': []}
            with patch.object(store.auth, 'authorize', return_value=user), \
                 patch.object(web, 'read_json_response', return_value={'result': 0, 'data': []}):
                with self.assertRaises(web.SettingsError): store.authorize({'warehouseId': '1'})
            self.assertFalse(web.may_retry(user, 1))
            self.assertTrue(web.may_retry({'roles': [{'name': web.ROLE, 'enabled': True, 'companyId': 1}]}, 1))
            self.assertFalse(web.may_retry({'roles': [{'name': web.ROLE, 'enabled': True, 'companyId': 2}]}, 1))

    def test_batch_inspection_retains_original_rows_and_rounding_audit(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp); store = self.store(path)
            (path / 'int_workorder_demo.csv').write_bytes(CONTENT.replace(b',10,', b',10.25,'))
            result = store.detail('int_workorder_demo.csv', 'WO-TEST-001')
            self.assertEqual(result['originalRows'][0]['PLANNED_QUANTITY'], '10.25')
            self.assertEqual(result['converted'][0]['payload']['expectedQuantity'], 11)

    def test_listing_only_shows_exceptions_and_preserves_full_batch_details(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp); store = self.store(path)
            name = 'int_workorder_demo.csv'
            states = ['BLOCKED', 'BUSINESS_ERROR', 'UNCERTAIN', 'REJECTED',
                      'PREPARED', 'ACCEPTED', 'COMPLETED', 'SKIPPED_EXISTING']
            records = [{'workOrderNumber': 'WO-' + state, 'state': state} for state in states]
            (path / (name + '.report.json')).write_text(json.dumps({'file': name, 'records': records}))
            self.assertEqual({r['state'] for r in store.listing({})['rows']}, web.EXCEPTION_STATES)
            self.assertEqual(store.listing({'state': ['COMPLETED']})['total'], 0)
            self.assertEqual(store.listing({'number': ['BUSINESS_ERROR']})['total'], 1)
            self.assertEqual(store.listing({'state': ['BLOCKED']})['total'], 1)
            self.assertEqual(len(store.detail(name)['report']['records']), len(states))
            (path / (name + '.report.json')).write_text(json.dumps({'file': name, 'state': 'REJECTED', 'reason': 'Invalid header'}))
            self.assertEqual(store.listing({})['rows'][0]['validationError'], 'Invalid header')
            (path / (name + '.report.json')).write_text(json.dumps({'file': name, 'state': 'NO_NEW_WORK_ORDERS'}))
            self.assertEqual(store.listing({})['total'], 0)

    def test_daily_report_refresh_does_not_erase_operator_exception_details(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp); store = self.store(path)
            row = {'recordId': '1', 'state': 'BLOCKED', 'validationError': 'Missing RM-001', 'missingItems': ['RM-001']}
            (store.audit_reports / 'int_workorder_demo.csv.json').write_text(json.dumps({'records': [row]}))
            row = {'recordId': '1', 'state': 'BLOCKED'}
            report = path / 'int_workorder_demo.csv.report.json'
            report.write_text(json.dumps({'file': 'int_workorder_demo.csv', 'records': [row]}))
            self.assertEqual(store.report('int_workorder_demo.csv')['records'][0]['missingItems'], ['RM-001'])
            row['state'] = 'COMPLETED'
            report.write_text(json.dumps({'file': 'int_workorder_demo.csv', 'records': [row]}))
            self.assertNotIn('validationError', store.report('int_workorder_demo.csv')['records'][0])

    def test_background_refresh_updates_mes_result_without_resubmitting(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp); store = self.store(path)
            record = self.records()[0]
            ledger = a.Ledger(path / 'ledger.sqlite3'); ledger.prepare([record])
            ledger.set('work-orders', record['recordId'], 'ACCEPTED', '42', 'PENDING'); ledger.db.close()
            report = {'file': 'int_workorder_demo.csv', 'records': [{'recordId': record['recordId'], 'state': 'ACCEPTED'}]}
            (path / 'int_workorder_demo.csv.report.json').write_text(json.dumps(report))
            api = API(); api.business_status = 'COMPLETED'
            with patch.object(a, 'MesAPI', return_value=api):
                self.assertTrue(store.refresh_processing())
            self.assertEqual(api.calls, [])
            self.assertEqual(store.report('int_workorder_demo.csv')['records'][0]['state'], 'COMPLETED')


if __name__ == '__main__':
    unittest.main()
