import io,json,threading,unittest,urllib.request,urllib.error
from datetime import datetime
from unittest.mock import patch
from openpyxl import load_workbook
from activity_core import aggregate,location_map,classify
from activity_api import remember,saved,selected,excel,Handler,CACHE,ReportError
from report_core import range_bounds
from http.server import ThreadingHTTPServer

def locations():
 return location_map([dict(name=n,isVirtual=v,isRF=r,isStorage=s) for n,v,r,s in [('STAGE',0,0,0),('RF1',0,1,0),('A1',0,0,1),('A2',0,0,1),('LINE',0,0,0),('VIRTUAL',1,0,0)]])
def event(i=1,kind='INVENTORY_MOVEMENT',source='RF1',dest='A1',user='DRIVER'):
 return dict(id=i,type=kind,from_value=source,to_value=dest,username=user,timestamp=datetime(2026,10,26,14,0),lpn='LPN1',quantity=12)
class Rules(unittest.TestCase):
 def test_two_leg_transfer_counts_only_deposit_and_repeated_jobs_remain(self):
  rows=[event(1,source='STAGE',dest='RF1'),event(2),event(3)]
  people,events=aggregate(rows,locations())
  self.assertEqual(people[0]['placement'],2);self.assertEqual(people[0]['excluded'],1)
  self.assertEqual(len(events),3)
 def test_virtual_system_and_shared_accounts_do_not_become_individual_drivers(self):
  p,e=aggregate([event(1,source='VIRTUAL',dest='LINE',user='CWMS_INNER_CALL'),event(2,user='Shipping'),event(3,user=None),event(4,user='Anonymous')],locations())
  self.assertEqual({x['group'] for x in p},{'system','shared','unknown'})
  self.assertEqual(next(x for x in p if x['group']=='system')['excluded'],1)
 def test_storage_to_storage_is_movement_not_placement(self):
  self.assertEqual(classify(event(source='A1',dest='A2'),locations())[0],'movement')
 def test_unknown_and_ambiguous_locations_are_not_guessed(self):
  self.assertEqual(classify(event(dest='MISSING'),locations())[0],'excluded')
  m=locations();m['A1']*=2
  self.assertEqual(classify(event(),m)[0],'excluded')
 def test_receiving_picking_not_double_counted_with_movement(self):
  p,e=aggregate([event(1,'RECEIVING'),event(2,'PICKING'),event(3,source='A1',dest='LINE')],locations())
  self.assertEqual([p[0][k] for k in ('receiving','placement','movement','picking')],[1,0,1,1])
 def test_repeated_rows_deduplicated_by_ledger_identity_not_lpn(self):
  p,e=aggregate([event(),event(),event(2)],locations());self.assertEqual(len(e),2)
 def test_utc_event_is_unambiguous_and_dst_range_uses_site_clock(self):
  p,e=aggregate([event()],locations());self.assertEqual(e[0]['time'],'2026-10-26T14:00:00+00:00')
  a,b=range_bounds('2026-10-31T18:00','2026-11-01T06:00','America/Los_Angeles');self.assertEqual((b-a).total_seconds(),13*3600)
  with self.assertRaises(ValueError):range_bounds('2026-11-01T01:30','2026-11-01T03:00','America/Los_Angeles')
class Snapshots(unittest.TestCase):
 def setUp(self):CACHE.clear()
 def fixture(self):
  p,e=aggregate([event(user='=BAD()'),event(2,user='SECOND')],locations())
  r=dict(rows=p,timezone='America/Los_Angeles',startTime='2026-10-26T13:00:00+00:00',endTime='2026-10-27T01:00:00+00:00')
  key=remember('owner',r,e)['snapshotId'];return key,saved(key,'owner')
 def test_owner_expiry_and_user_selection(self):
  key,e=self.fixture()
  with self.assertRaises(ReportError):saved(key,'other')
  with self.assertRaises(ReportError):selected(e,'["not-in-snapshot"]')
  CACHE[key]['expires']=0
  with self.assertRaises(ReportError):saved(key,'owner')
 def test_export_matches_selected_snapshot_and_neutralizes_formulas(self):
  _,e=self.fixture();key=e['result']['rows'][0]['key'];book=load_workbook(io.BytesIO(excel(e,{key})))
  self.assertTrue(book['Users']['A2'].value.endswith('-07:00'));self.assertEqual(book['Users'].max_row,4);self.assertEqual(book['Details'].max_row,2)
  self.assertEqual(book['Users']['A4'].value,"'=BAD()")
  self.assertTrue(book['Details']['A2'].value.endswith('-07:00'))
 def test_http_denies_unauthed_cross_site_and_duplicate_parameters(self):
  server=ThreadingHTTPServer(('127.0.0.1',0),Handler);worker=threading.Thread(target=server.serve_forever,daemon=True);worker.start()
  base='http://127.0.0.1:'+str(server.server_port)
  try:
   for path,status in [('/user-activity/config?companyId=1&warehouseId=1',401),('/user-activity/config?companyId=1&warehouseId=2',403),('/user-activity/config?companyId=1&companyId=2&warehouseId=1',400)]:
    with self.assertRaises(urllib.error.HTTPError) as err:urllib.request.urlopen(base+path)
    self.assertEqual(err.exception.code,status)
  finally:server.shutdown();server.server_close();worker.join()

class Permissions(unittest.TestCase):
 @patch.dict('os.environ',{'QC_GATEWAY_URL':'http://gateway/api'})
 @patch('requests.get')
 def test_inventory_permission_required_and_gateway_identity_checked(self,get):
  import base64,time
  from unittest.mock import Mock
  from activity_auth import authorize
  payload=base64.urlsafe_b64encode(json.dumps(dict(companyId=1,sub='MANAGER',exp=time.time()+3600)).encode()).decode().rstrip('=')
  token='Bearer e30.'+payload+'.unit-only'
  info=dict(user=dict(id=1,username='MANAGER',companyId=1),menu=[dict(link='/qc/result')])
  response=Mock(status_code=200);response.json.return_value=dict(result=0,data=info);get.return_value=response
  with self.assertRaises(ReportError):authorize(token,1,1)
  info['menu']=[dict(link='/inventory/inventory-activity')]
  self.assertTrue(authorize(token,1,1))
  info['menu'][0]['disabled']=True
  with self.assertRaises(ReportError):authorize(token,1,1)
  info['menu'][0]['disabled']=False;info['user']['username']='OTHER'
  with self.assertRaises(ReportError):authorize(token,1,1)


class ScopeAndLimits(unittest.TestCase):
 def test_client_restricted_warehouse_fails_closed(self):
  from contextlib import contextmanager
  from activity_api import query
  from unittest.mock import Mock
  cursor=Mock();cursor.fetchall.return_value=[dict(three_party_logistics_flag=1)]
  @contextmanager
  def db():yield cursor
  with patch('activity_api.database',db),patch('activity_api.load_warehouse_timezone',return_value='America/Los_Angeles'),self.assertRaises(ReportError):query(1,1,'2026-10-23T06:00','2026-10-23T18:00')
 def test_cap_refuses_incomplete_totals(self):
  from contextlib import contextmanager
  from activity_api import query
  from unittest.mock import Mock
  cursor=Mock();cursor.fetchall.side_effect=[[dict(three_party_logistics_flag=0)],[],[None]*50001]
  @contextmanager
  def db():yield cursor
  with patch('activity_api.database',db),patch('activity_api.load_warehouse_timezone',return_value='America/Los_Angeles'),self.assertRaises(ReportError) as err:query(1,1,'2026-10-23T06:00','2026-10-23T18:00')
  self.assertEqual(err.exception.status,422)
if __name__=='__main__':unittest.main()
