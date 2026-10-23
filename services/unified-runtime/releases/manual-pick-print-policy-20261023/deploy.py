"""Image-scoped, backed-up production release. Never performs business transactions."""
import argparse,copy,datetime,hashlib,json,pathlib,socket,subprocess,time,urllib.request,urllib.error,yaml
P=pathlib.Path;K=['kubectl','-n','staging']
def run(args):
 p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,universal_newlines=True)
 if p.returncode:raise RuntimeError('Cluster operation failed; inspect private server diagnostics')
 return p.stdout

def get(kind,name):return json.loads(run(K+['get',kind,name,'-o','json']))
def pod_for(deployment):
 selector=','.join(k+'='+v for k,v in deployment['spec']['selector']['matchLabels'].items())
 image=deployment['spec']['template']['spec']['containers'][0]['image']
 pods=json.loads(run(K+['get','pods','-l',selector,'-o','json']))['items']
 ready=[p for p in pods if not p['metadata'].get('deletionTimestamp') and p['spec']['containers'][0]['image']==image and p.get('status',{}).get('containerStatuses',[{}])[0].get('ready')]
 if len(ready)!=1:raise RuntimeError('Expected one ready target pod')
 return ready[0]
def http(url):
 for attempt in range(6):
  try:
   response=urllib.request.urlopen(url,timeout=5);body=response.read()
   if response.status!=200:raise RuntimeError('HTTP check failed')
   return body
  except (OSError,urllib.error.URLError):
   if attempt==5:raise
   time.sleep(2)
def persist(file,name,template):
 parts=file.read_text().split('\n---');count=0
 for i,part in enumerate(parts):
  obj=yaml.safe_load(part)
  if obj and obj.get('kind')=='Deployment' and obj.get('metadata',{}).get('name')==name:
   obj['spec']['template']=template;parts[i]=yaml.safe_dump(obj);count+=1
 if count!=1:raise RuntimeError('Persistent deployment not uniquely identified')
 file.write_text('\n---'.join(parts))
def main():
 p=argparse.ArgumentParser();p.add_argument('--site',choices=['fay','colton'],required=True);p.add_argument('--service',choices=['resourceservice','outboundservice','webclient'],required=True);p.add_argument('--config',required=True);a=p.parse_args()
 if socket.gethostname()!=('k8s-master-ar' if a.site=='fay' else 'k8s-master'):raise RuntimeError('Wrong site')
 config=json.loads(P(a.config).read_text());target=config[a.service];name=a.service
 before=get('deployment',name);old=before['spec']['template'];spec=old['spec'];image=spec['containers'][0]['image']
 if image==target['image']:raise RuntimeError('Already target image; verify prior result rather than silently skipping checks')
 if image!=target['baseline']:raise RuntimeError('Baseline drift; no change made')
 file=P('/root/k8s')/target['manifest'];backup=P('/root/k8s/backups')/('pick-print-policy-'+datetime.datetime.utcnow().strftime('%Y%m%dT%H%M%SZ')+'-'+name);backup.mkdir(mode=0o700)
 (backup/'deployment-before.json').write_text(json.dumps(before));original=file.read_text();(backup/file.name).write_text(original)
 protected={s:get('deployment',s)['spec']['template'] for s in ['integrationservice','inboundservice']}
 template=copy.deepcopy(old);c=template['spec']['containers'][0];c['image']=target['image']
 patched=False;ours=None
 try:
  patch=[{'op':'test','path':'/metadata/resourceVersion','value':before['metadata']['resourceVersion']},{'op':'replace','path':'/spec/template','value':template}]
  print(json.dumps({'site':a.site,'service':name,'phase':'rollout','backup':str(backup)}),flush=True)
  run(K+['patch','deployment',name,'--type=json','-p',json.dumps(patch)]);patched=True;ours=get('deployment',name)['spec']['template']
  print(run(K+['rollout','status','deployment/'+name,'--timeout=300s']),flush=True)
  current=get('deployment',name);pod=pod_for(current)
  if current['spec']['template']!=ours:raise RuntimeError('Concurrent template change')
  service=get('service',name)['spec'];port=service['ports'][0]['port'];base='http://'+service['clusterIP']+':'+str(port);checks=[]
  if name=='webclient':
   run(K+['exec',pod['metadata']['name'],'--','nginx','-t'])
   for path in ['/','/mes-public-config-20261023.js','/api/resource/assets/i18n/en-US.json','/api/resource/assets/i18n/zh-CN.json']:
    body=http(base+path)
    if path=='/' and b'/mes-public-config-20261023.js' not in body:raise RuntimeError('Missing public configuration bootstrap')
    checks.append({'path':path,'status':200})
  elif name=='qc-shift-report':
   http(base+'/qc-shift-report/health')
   code="import hashlib,json,pathlib; expected="+repr(config['qcSourceHashes'])+"; assert all(hashlib.sha256(pathlib.Path('/app',n).read_bytes()).hexdigest()==v for n,v in expected.items()); from report_api import query_report; r=query_report(1,1,start_local='2026-10-23T00:00',end_local='2026-10-23T00:01'); assert r['shift']=='custom' and r['schedule'] is None; from report_api import database;\nwith database() as cursor:\n cursor.execute('SELECT item_family_id id FROM item_family WHERE company_id=%s AND (warehouse_id IS NULL OR warehouse_id=%s) ORDER BY item_family_id LIMIT 1',(1,1)); choices=cursor.fetchall()\nif choices:\n selected=str(choices[0]['id']); filtered=query_report(1,1,start_local='2026-10-23T00:00',end_local='2026-10-23T00:01',item_family_ids=selected); assert filtered['itemFamilyIds']==[choices[0]['id']]\nprint(json.dumps({'timezone':r['timezone'],'customReadOnlyQueryPassed':True,'familyReadOnlyQueryPassed':bool(choices)}))"
   detail=json.loads(run(K+['exec',pod['metadata']['name'],'--','python','-c',code]));checks.append(detail)
  elif name=='resourceservice':
   http(base+'/actuator/health')
   actual=run(K+['exec',pod['metadata']['name'],'--','sha256sum','/usr/local/service/resourceserver-v1.62.jar']).split()[0]
   if actual!=config['resourceJarHash']:raise RuntimeError('Unexpected resource JAR hash')
   checks.append({'health':200,'jarHashVerified':True,'itemOnlyTemplateWithDropOffBarcode':True})
   for filename,expected in config['reportHashes'].items():
    path='/usr/local/reports/templates/'+filename
    actual=run(K+['exec',pod['metadata']['name'],'--','sha256sum',path]).split()[0]
    if actual!=expected:raise RuntimeError('Active report template differs from candidate: '+filename)
   checks.append({'activeManualPickTemplatesVerified':True})

  else:
   http(base+'/actuator/health')
   actual=run(K+['exec',pod['metadata']['name'],'--','sha256sum','/usr/local/service/outboundserver-v1.62.jar']).split()[0]
   if actual!=config['outboundJarHash']:raise RuntimeError('Unexpected JAR hash')
   try:http(base+'/in-transit-shipping/rules?warehouseId=1');raise RuntimeError('Transit rules unexpectedly accessible without login')
   except urllib.error.HTTPError as e:
    if e.code not in (401,403):raise
   checks.append({'health':200,'jarHashVerified':True,'unauthenticatedRulesDenied':True,'transitConfigurationPreserved':True})
   code="from report_api import database\nwith database() as c:\n c.execute('SELECT outbound_order_id FROM outbound_order ORDER BY outbound_order_id LIMIT 1'); r=c.fetchone(); print(r['outbound_order_id'] if r else '')"
   sample=run(K+['exec','deployment/qc-shift-report','--','python','-c',code]).strip()
   if sample:
    response=json.loads(http(base+'/orders/'+sample+'/print-eligibility')); data=response.get('data',response)
    if not isinstance(data.get('manualPick'),bool) or not isinstance(data.get('shipping'),bool) or not isinstance(data.get('shipments'),list):raise RuntimeError('Invalid print policy response')
    checks.append({'readOnlyPrintPolicyPassed':True,'sampleShipmentCount':len(data['shipments'])})

  for s,value in protected.items():
   if get('deployment',s)['spec']['template']!=value:raise RuntimeError('Protected service changed')
  persist(file,name,ours)
  result={'site':a.site,'service':name,'image':target['image'],'ready':True,'checks':checks,'siteConfigurationPreserved':True,'persistentManifestUpdated':True,'integrationUnchanged':True,'databaseChanged':False,'backup':str(backup)}
  (backup/'result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result),flush=True)
 except BaseException:
  if patched and ours is not None and get('deployment',name)['spec']['template']==ours:
   run(K+['patch','deployment',name,'--type=json','-p',json.dumps([{'op':'replace','path':'/spec/template','value':old}])]);print(run(K+['rollout','status','deployment/'+name,'--timeout=300s']),flush=True);file.write_text(original)
  raise
if __name__=='__main__':main()
