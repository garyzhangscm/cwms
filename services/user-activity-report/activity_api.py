"""Read-only Warehouse User Activity service. No database writes."""
import io,json,logging,os,secrets,threading,time
from datetime import datetime,timezone
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from urllib.parse import urlsplit,parse_qs
from report_api import database,positive,ReportError,load_warehouse_timezone
from report_core import range_bounds
from activity_auth import authorize
from activity_core import aggregate,location_map,KINDS
CACHE={};LOCK=threading.Lock();SLOTS=threading.BoundedSemaphore(2)
MAX_ROWS=50000

def query(company,warehouse,start,end):
    if warehouse!=1 or company!=1:raise ReportError(403,'This report supports the configured WMEC warehouse only')
    with database() as c:
        zone=load_warehouse_timezone(c,company,warehouse)
        c.execute('SELECT three_party_logistics_flag FROM warehouse_configuration WHERE warehouse_id=%s',(warehouse,))
        config=c.fetchall()
        if len(config)!=1 or config[0]['three_party_logistics_flag']!=0:raise ReportError(403,'This report supports non-3PL WMEC only; client-restricted reporting requires separate configuration')
        try:first,last=range_bounds(start,end,zone)
        except ValueError as e:raise ReportError(400,str(e)) from None
        c.execute('''SELECT l.name,t.virtual_locations isVirtual,t.rf_locations isRF,
          t.storage_locations isStorage FROM location l
          JOIN location_group g ON g.location_group_id=l.location_group_id
          JOIN location_group_type t ON t.location_group_type_id=g.location_group_type_id
          WHERE l.warehouse_id=%s''',(warehouse,))
        locations=location_map(c.fetchall())
        # No date/user index exists. One capped scan, bounded by database execution timeout;
        # never silently return partial totals. No joins against the large activity ledger.
        c.execute('''SELECT inventory_activity_id id,type,username,activity_datetime timestamp,
          lpn,quantity,from_value,to_value,document_number document,rf_code rf,item_id,location_id
          FROM inventory_activity WHERE warehouse_id=%s AND activity_datetime>=%s
          AND activity_datetime<%s AND type IN ('RECEIVING','PICKING','INVENTORY_MOVEMENT')
          LIMIT %s''',(warehouse,first.replace(tzinfo=None),last.replace(tzinfo=None),MAX_ROWS+1))
        rows=c.fetchall()
        if len(rows)>MAX_ROWS:raise ReportError(422,'Over 50,000 activities. Select a shorter period; no partial totals are shown.')
        ids=sorted({r['item_id'] for r in rows if r['item_id']})
        products={}
        if ids:
            c.execute('SELECT item_id id,name FROM item WHERE item_id IN ('+','.join(['%s']*len(ids))+')',ids)
            products={p['id']:p['name'] for p in c.fetchall()}
        c.execute('SELECT location_id id,name FROM location WHERE warehouse_id=%s',(warehouse,))
        names={p['id']:p['name'] for p in c.fetchall()}
        for r in rows:r.update(product=products.get(r['item_id'],''),location=names.get(r['location_id'],''))
    people,events=aggregate(rows,locations)
    return dict(warehouseId=warehouse,timezone=zone,startTime=first.isoformat(),endTime=last.isoformat(),
      generatedAt=datetime.now(timezone.utc).isoformat(),rows=people,eventCount=len(events),
      notes=['Counts are completed inventory activity events, not scans or shifts worked.',
        'Placement means a non-storage location (including RF) into storage. It may include receiving, production or returns.',
        'RF pickup legs, virtual transitions and ambiguous locations are excluded from completed work; system/unknown accounts are separate.',
        'Picking uses UTC inventory activities only; legacy pick confirmation local timestamps are not mixed into this report.',
        'Times are server completion times. Historical raw scans, offline scan times and business-page labels are unavailable.']),events

def remember(owner,result,events):
    key=secrets.token_urlsafe(24)
    with LOCK:
        now=time.monotonic()
        for k in list(CACHE):
            if CACHE[k]['expires']<=now:del CACHE[k]
        if len(CACHE)>=12:raise ReportError(503,'Report cache is busy. Try again shortly.')
        result['snapshotId']=key
        CACHE[key]=dict(owner=owner,result=result,events=events,expires=now+900)
    return result

def saved(key,owner):
    with LOCK:entry=CACHE.get(key)
    if not entry or entry['owner']!=owner or entry['expires']<=time.monotonic():raise ReportError(410,'Saved result expired or inaccessible. Run the report again.')
    return entry

def selected(entry,raw):
    try:keys=json.loads(raw) if raw else [p['key'] for p in entry['result']['rows']]
    except ValueError:raise ReportError(400,'Invalid user selection') from None
    if not isinstance(keys,list) or len(keys)>500 or any(not isinstance(k,str) for k in keys) or not set(keys)<={p['key'] for p in entry['result']['rows']}:raise ReportError(400,'Invalid user selection')
    return set(keys)

def excel(entry,keys):
    from openpyxl import Workbook
    from zoneinfo import ZoneInfo
    b=Workbook(write_only=True);s=b.create_sheet('Users');r=entry['result'];zone=ZoneInfo(r['timezone'])
    def safe(v):return "'"+v if isinstance(v,str) and v[:1] in ('=','+','-','@') else v
    s.append(['Warehouse User Activity',r['timezone']]);s.append([datetime.fromisoformat(r[k]).astimezone(zone).isoformat() for k in ('startTime','endTime')]);s.append(['User','Account type',*KINDS])
    for p in r['rows']:
        if p['key'] in keys:s.append([safe(p['username']),p['group'],*[p[k] for k in KINDS]])
    d=b.create_sheet('Details');d.append(['Local time','User','Account type','Activity','LPN','Product','Quantity','From','To','Document','RF','Note'])
    for e in entry['events']:
        if e['userKey'] in keys:d.append([safe(x) for x in [datetime.fromisoformat(e['time']).astimezone(zone).isoformat(),e['username'],e['group'],e['kind'],e['lpn'],e['product'],e['quantity'],e['source'],e['destination'],e['document'],e['rf'],e['note']]])
    out=io.BytesIO();b.save(out);return out.getvalue()

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def send(self,status,body,mime='application/json; charset=utf-8'):
        data=json.dumps(body).encode() if not isinstance(body,bytes) else body
        self.send_response(status);self.send_header('Content-Type',mime);self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
    def do_GET(self):
        try:
            path=urlsplit(self.path)
            if path.path=='/user-activity/health':self.send(200,dict(result=0,data=dict(environment=os.environ.get('ACTIVITY_ENVIRONMENT','test'),readOnly=True)));return
            p=parse_qs(path.query,keep_blank_values=True)
            allowed={'companyId','warehouseId','start','end','snapshotId','user','kind','page','users'}
            if set(p)-allowed or any(len(v)!=1 for v in p.values()):raise ReportError(400,'Invalid parameters')
            p={k:v[0] for k,v in p.items()};company=positive(p.get('companyId'));warehouse=positive(p.get('warehouseId'))
            if company!=1 or warehouse!=1:raise ReportError(403,'This report supports the configured WMEC warehouse only')
            owner=authorize(self.headers.get('Authorization'),company,warehouse)
            if path.path=='/user-activity/config':
                with database() as c:zone=load_warehouse_timezone(c,company,warehouse)
                self.send(200,dict(result=0,data=dict(timezone=zone)));return
            if path.path=='/user-activity/report':
                if not SLOTS.acquire(blocking=False):raise ReportError(503,'Two reports are already running. Try again shortly.')
                try:result,events=query(company,warehouse,p.get('start'),p.get('end'));result=remember(owner,result,events)
                finally:SLOTS.release()
                self.send(200,dict(result=0,data=result));return
            if path.path in ('/user-activity/details','/user-activity/export'):
                entry=saved(p.get('snapshotId'),owner)
                if path.path.endswith('/export'):
                    self.send(200,excel(entry,selected(entry,p.get('users'))),'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');return
                if p.get('kind') not in KINDS or p.get('user') not in {r['key'] for r in entry['result']['rows']}:raise ReportError(400,'Invalid detail selection')
                page=positive(p.get('page','1'))
                rows=[e for e in entry['events'] if e['userKey']==p['user'] and e['kind']==p['kind']]
                self.send(200,dict(result=0,data=dict(total=len(rows),page=page,rows=rows[(page-1)*50:page*50])));return
            raise ReportError(404,'Not found')
        except ReportError as e:self.send(e.status,dict(result=e.status,message=e.message))
        except Exception:
            logging.exception('Activity report failed')
            self.send(503,dict(result=503,message='The report could not be completed. No partial totals are shown; try a shorter period.'))
    def do_POST(self):self.send(405,dict(result=405,message='Read-only service'))

if __name__=='__main__':
    if os.environ.get('ACTIVITY_ENVIRONMENT') not in ('test','production'):raise RuntimeError('Explicit activity environment required')
    ThreadingHTTPServer(('0.0.0.0',18796),Handler).serve_forever()
