import json
import copy
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import adapter as a
import item_import_api as web
from work_order_api import may_retry, SettingsError
from test_oracle_items import data, CONFIG as MAPPING
from test_oracle_ftp import FTP, API, NAME

CONTENT = data([['GOOD','Good','01','1','1'],['BAD','Bad','02','1','1']])
CONFIG = {'sourceFormat':'oracle-items-v1','ftp':{},'mes':{'baseUrl':'http://mes.test'},
          'oracleItems':{'mapping':MAPPING, 'companyId':1, 'warehouseId':1, 'inventoryBaseUrl':'http://inventory.test'},
          'operatorApi':{'authBaseUrl':'http://auth.test', 'userBaseUrl':'http://user.test', 'layoutBaseUrl':'http://layout.test', 'bind':'127.0.0.1', 'port':18793}}

class Guard:
    warehouse_id = 1
    def check(self, records): return set()

class ItemOperatorTests(unittest.TestCase):
    def store(self, path):
        (path/NAME).write_bytes(CONTENT)
        (path/(NAME+'.report.json')).write_text(json.dumps({'file':NAME,'records':[]}))
        return web.ItemStore(dict(copy.deepcopy(CONFIG), stateDirectory=str(path)))

    def test_mixed_batch_invalid_mapping_does_not_block_other_item(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp); ftp=FTP();ftp.files={NAME:CONTENT,NAME+'.ready':b''};api=API()
            with patch.object(a,'ftp_connect',return_value=ftp):
                a.poll_once(dict(CONFIG,itemExceptionHandling=True),path,api,Guard(),target_file=NAME)
            self.assertEqual(len(api.calls),1)
            report=json.loads((path/(NAME+'.report.json')).read_text())
            self.assertEqual([r['state'] for r in report['records']],['ACCEPTED','BLOCKED'])
            self.assertIn('unmapped item type',report['records'][1]['validationError'])
            self.assertIn(NAME,ftp.files)

    def test_check_only_no_submit_no_claim_no_delete_and_mapping_correction(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp);store=self.store(path);ftp=FTP();ftp.files={NAME:CONTENT,NAME+'.ready':b''};api=API()
            mapping=path/'mapping.json';mapping.write_text(json.dumps({'01':'Finish Good'}))
            store.config['oracleItems']['itemTypeMappingFile']=str(mapping)
            with patch.object(a,'ftp_connect',return_value=ftp):
                a.poll_once(store.config,path,api,Guard(),target_file=NAME,check_only=True)
            self.assertEqual(api.calls,[]); self.assertEqual(store.listing({})['total'],1)
            ledger=a.Ledger(path/'ledger.sqlite3')
            self.assertEqual(ledger.db.execute('SELECT count(*) FROM item_claims').fetchone()[0],0);ledger.db.close()
            mapping.write_text(json.dumps({'01':'Finish Good','02':'Raw Material'}))
            with patch.object(a,'ftp_connect',return_value=ftp):
                a.poll_once(store.config,path,api,Guard(),target_file=NAME,target_item='BAD',check_only=True)
            self.assertEqual(store.listing({})['total'],0);self.assertIn(NAME,ftp.files)
            self.assertEqual(api.calls,[])

    def test_selected_retry_does_not_submit_other_item_and_repeated_retry_no_duplicate(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp);store=self.store(path);ftp=FTP();ftp.files={NAME:CONTENT,NAME+'.ready':b''};api=API()
            with patch.object(a,'ftp_connect',return_value=ftp):
                for _ in range(2):
                    a.poll_once(store.config,path,api,Guard(),target_file=NAME,target_item='GOOD',retry_failed=True)
            self.assertEqual(len(api.calls),1)
            self.assertEqual(store.listing({})['total'],1)

    def test_uncertain_no_blind_retry_and_verified_error_resends_same_id(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp);store=self.store(path)
            record=store.detail(NAME,'GOOD')['converted'][0]
            ledger=a.Ledger(path/'ledger.sqlite3');ledger.prepare([record]);api=API()
            api.recover_item=lambda *args: None
            ledger.set('items',record['recordId'],'UNCERTAIN')
            a.process_item_records([record],ledger,api,Guard(),retry_failed=True)
            self.assertEqual(api.calls,[]);self.assertEqual(ledger.get('items',record['recordId'])['state'],'UNCERTAIN')
            ledger.set('items',record['recordId'],'BUSINESS_ERROR','42','ERROR')
            api.recover_item=lambda *args: {'id':42,'status':'ERROR'}
            sent=[];api.resend_item=sent.append
            a.process_item_records([record],ledger,api,Guard(),retry_failed=True)
            self.assertEqual(sent,['42']);self.assertEqual(api.calls,[]);ledger.db.close()

    def test_existing_item_does_not_hide_failed_integration(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp);store=self.store(path);record=store.detail(NAME,'GOOD')['converted'][0]
            ledger=a.Ledger(path/'ledger.sqlite3');ledger.prepare([record])
            ledger.set('items',record['recordId'],'BUSINESS_ERROR','42','ERROR')
            guard=Guard();guard.check=lambda records: {'GOOD'};api=API()
            a.process_item_records([record],ledger,api,guard,check_only=True)
            self.assertEqual(ledger.get('items',record['recordId'])['state'],'BUSINESS_ERROR')
            api.recover_item=lambda *args: {'id':42,'status':'ERROR'};sent=[];api.resend_item=sent.append
            a.process_item_records([record],ledger,api,guard,retry_failed=True)
            self.assertEqual(sent,['42']);self.assertEqual(api.calls,[]);ledger.db.close()

    def test_queue_selection_detail_audit_and_item_role(self):
        with tempfile.TemporaryDirectory() as temp:
            store=self.store(Path(temp));user={'username':'alice'}
            job=store.enqueue({'file':NAME,'itemName':'GOOD','action':'check'},user)
            self.assertEqual(store.detail(NAME,'GOOD')['history'][0]['actor'],'alice')
            self.assertEqual(len(store.detail(NAME,'GOOD')['originalRows']),1)
            with self.assertRaises(SettingsError):store.enqueue({'file':NAME,'workOrderNumber':'GOOD','action':'retry'},user)
            with self.assertRaises(SettingsError):store.enqueue({'file':NAME,'itemName':'OTHER','action':'retry'},user)
            with self.assertRaises(SettingsError):store.enqueue({'file':'../x','action':'check'},user)
            with patch.object(a,'poll_once') as poll:
                self.assertTrue(store.run_next())
                self.assertEqual(poll.call_args[1]['target_item'],'GOOD')
                self.assertTrue(poll.call_args[1]['check_only'])
            role={'roles':[{'name':'Item Import Operator','enabled':True,'companyId':1}]}
            self.assertTrue(may_retry(role,1,store.role));self.assertFalse(may_retry(role,2,store.role))
            self.assertFalse(may_retry(role,1))

    def test_recovery_requires_exact_payload_scope_and_unique_match(self):
        with tempfile.TemporaryDirectory() as temp:
            record=self.store(Path(temp)).detail(NAME,'GOOD')['converted'][0]
            api=a.MesAPI('http://mes.test',company_code='C')
            match=dict(record['payload'],id=42,status='ERROR',warehouseId=1)
            with patch.object(api,'request',return_value=match):self.assertEqual(api.recover_item(record,1,'42')['id'],42)
            with patch.object(api,'request',return_value=dict(match,warehouseId=None)):
                self.assertEqual(api.recover_item(record,1,'42')['id'],42)
            for row in (dict(match,description='changed'),dict(match,warehouseId=2),dict(match,id=43)):
                with patch.object(api,'request',return_value=row):self.assertIsNone(api.recover_item(record,1,'42'))
            with patch.object(api,'request',return_value=[match,match]):self.assertIsNone(api.recover_item(record,1))

    def test_legacy_daily_importer_continues_after_operator_retained_bad_snapshot(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp);ftp=FTP();bad='int_item_bad.csv';(path/bad).write_bytes(b'WRONG\n')
            ftp.files={bad:b'WRONG\n',bad+'.ready':b'',NAME:data([['GOOD','Good','01','1','1']]),NAME+'.ready':b''};api=API()
            with patch.object(a,'ftp_connect',return_value=ftp):a.poll_once(CONFIG,path,api,Guard())
            self.assertEqual(len(api.calls),1)
            self.assertEqual(json.loads((path/(bad+'.report.json')).read_text())['state'],'REJECTED')

    def test_malformed_batch_retains_source_and_normal_rows_continue(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp);ftp=FTP();bad='int_item_bad.csv';ftp.files={bad:b'WRONG\n',bad+'.ready':b'',NAME:CONTENT,NAME+'.ready':b''};api=API()
            with patch.object(a,'ftp_connect',return_value=ftp):a.poll_once(dict(CONFIG,itemExceptionHandling=True),path,api,Guard())
            self.assertEqual((path/bad).read_bytes(),b'WRONG\n');self.assertEqual(len(api.calls),1)
            self.assertEqual(json.loads((path/(bad+'.report.json')).read_text())['state'],'REJECTED')

if __name__=='__main__':unittest.main()
