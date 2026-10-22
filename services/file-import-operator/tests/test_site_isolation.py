import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import adapter as a
from test_oracle_ftp import CONFIG, CONTENT, NAME, FTP, API, Guard

class SiteIsolation(unittest.TestCase):
    def config(self):
        value=copy.deepcopy(CONFIG)
        value['oracleItems']['filenamePrefix']='fayint_item'
        value['deleteSourceFiles']=False
        return value
    def test_default_prefix_remains_exactly_colton(self):
        for name in [NAME,'int_item_bad.csv','fayint_item_20261022.csv','int_item__.csv','../int_item_x.csv']:
            self.assertEqual(a.published_file(name,CONFIG),a.ORACLE_ITEMS_RE.fullmatch(name) is not None)
    def test_fay_accepts_only_its_prefix(self):
        config=self.config()
        self.assertTrue(a.published_file('fayint_item_20261022.csv',config))
        for name in [NAME,'other_fayint_item_20261022.csv','../fayint_item_x.csv','fayint_item_x.csv.ready']:
            self.assertFalse(a.published_file(name,config))
    def test_invalid_prefix_fails_before_network(self):
        for value in ['../int_item','',None,'*','x'*41]:
            config=self.config();config['oracleItems']['filenamePrefix']=value
            with tempfile.TemporaryDirectory() as temp,patch.object(a,'ftp_connect') as connect:
                with self.assertRaises(ValueError):a.poll_once(config,Path(temp),API(),Guard())
                connect.assert_not_called()
    def test_fay_preserves_source_files_and_continues_to_later_batches(self):
        config=self.config();ftp=FTP();api=API();guard=Guard();name='fayint_item_20261022.csv'
        ftp.files[name]=CONTENT;ftp.files[name+'.ready']=b''
        with tempfile.TemporaryDirectory() as temp,patch.object(a,'ftp_connect',return_value=ftp):
            state=Path(temp);a.poll_once(config,state,api,guard)
            self.assertEqual(len(api.calls),3);self.assertFalse((state/NAME).exists())
            api.business_status='COMPLETED';a.poll_once(config,state,api,guard)
            self.assertIn(name,ftp.files);self.assertIn(name+'.ready',ftp.files)
            self.assertEqual(json.loads((state/(name+'.report.json')).read_text())['sourceCleanup'],'DISABLED')
            later='fayint_item_20261023.csv';ftp.files[later]=CONTENT.replace(b'TEST-ITEM-',b'TEST-NEXT-');ftp.files[later+'.ready']=b''
            a.poll_once(config,state,api,guard)
            self.assertEqual(len(api.calls),6);self.assertIn(name,ftp.files);self.assertIn(NAME,ftp.files)
    def test_invalid_cleanup_flag_fails_before_network(self):
        config=self.config();config['deleteSourceFiles']='false'
        with tempfile.TemporaryDirectory() as temp,patch.object(a,'ftp_connect') as connect:
            with self.assertRaises(ValueError):a.poll_once(config,Path(temp),API(),Guard())
            connect.assert_not_called()
