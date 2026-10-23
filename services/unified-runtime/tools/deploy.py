"""Backed-up rollout of shared MES images with explicit site flags; Integration is excluded."""
import argparse,datetime,json,pathlib,subprocess,time,urllib.request,urllib.error,socket,yaml
P=pathlib.Path;K=['kubectl','-n','staging']
def run(args):
 p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,universal_newlines=True)
 if p.returncode:raise RuntimeError('Kubernetes operation failed ('+str(p.returncode)+'); inspect server-side status without logging private arguments')
 return p.stdout
def get(kind,name):return json.loads(run(K+['get',kind,name,'-o','json']))
def ready_pod(deployment):
 selector=deployment['spec']['selector']['matchLabels'];query=','.join(k+'='+v for k,v in selector.items());pods=json.loads(run(K+['get','pods','-l',query,'-o','json']))['items'];image=deployment['spec']['template']['spec']['containers'][0]['image']
 ready=[p for p in pods if not p['metadata'].get('deletionTimestamp') and p['spec']['containers'][0]['image']==image and p.get('status',{}).get('containerStatuses',[{}])[0].get('ready')]
 if len(ready)!=1:raise RuntimeError('Expected exactly one ready target-image pod')
 return ready[0]
def http_health(service):
 s=get('service',service)['spec'];port=s['ports'][0]['port'];host=s['clusterIP'];path='/probe/live' if service=='inventoryservice' else '/actuator/health';last=''
 for attempt in range(24):
  try:
   with socket.create_connection((host,port),timeout=3):pass
   try:r=urllib.request.urlopen('http://'+host+':'+str(port)+path,timeout=5)
   except urllib.error.HTTPError as e:r=e
   status=r.code;r.close()
   if status in [200,401,403,404]:return {'tcp':True,'httpStatus':status,'path':path}
   last=str(status)
  except Exception as e:last=type(e).__name__
  time.sleep(3)
 raise RuntimeError('Service health not ready: '+last)
def manifest(name):
 found=[]
 for p in [P('/root/k8s/3-framework.yaml'),P('/root/k8s/5-backend-app.yaml')]:
  docs=list(yaml.safe_load_all(p.read_text()))
  if any(d and d.get('kind')=='Deployment' and d.get('metadata',{}).get('name')==name for d in docs):found.append(p)
 if len(found)!=1:raise RuntimeError('Persistent deployment manifest is not uniquely identified: '+name)
 return found[0]
def persist(file,name,image,flags,podspec):
 docs=list(yaml.safe_load_all(file.read_text()));count=0
 for d in docs:
  if not d or d.get('kind')!='Deployment' or d.get('metadata',{}).get('name')!=name:continue
  count+=1;s=d['spec']['template']['spec'];c=s['containers'][0];c['image']=image
  # Preserve already-deployed site mounts and environment, including OTA/report paths.
  c['env']=podspec['containers'][0].get('env',[])
  c['volumeMounts']=podspec['containers'][0].get('volumeMounts',[])
  s['volumes']=podspec.get('volumes',[])
  s['nodeSelector']=podspec['nodeSelector'];c['readinessProbe']=podspec['containers'][0]['readinessProbe']
 if count!=1:raise RuntimeError('Persistent Deployment not uniquely found')
 file.write_text(yaml.safe_dump_all(docs))
def main():
 p=argparse.ArgumentParser();p.add_argument('--site',choices=['colton','fay'],required=True);p.add_argument('--config',type=P,required=True);p.add_argument('--services',nargs='+');a=p.parse_args();targets=json.loads((a.config/'targets.json').read_text());sites=json.loads((a.config/'sites.json').read_text());baselines={d['name']:d['image'] for d in json.loads((a.config/'baselines.json').read_text())[a.site]}
 if socket.gethostname()!=('k8s-master-ar' if a.site=='fay' else 'k8s-master'):raise RuntimeError('Wrong factory master')
 order=a.services or ['resourceservice','inboundservice','inventoryservice','outboundservice','commonservice','adminservice','authserver','apigateway','layoutservice']
 if not set(order)<=set(targets) or 'integrationservice' in order:raise RuntimeError('Unapproved deployment scope')
 integration=get('deployment','integrationservice')['spec']['template'];stamp=datetime.datetime.utcnow().strftime('%Y%m%dT%H%M%SZ');backup=P('/root/k8s/backups')/('unified-runtime-'+stamp);backup.mkdir(mode=0o700);results=[]
 for name in order:
  before=get('deployment',name);template=before['spec']['template'];oldspec=template['spec'];oldimage=oldspec['containers'][0]['image'];target=targets[name];flags=dict(sites[a.site].get(name,{}))
  if name!='apigateway':flags['SPRING_JPA_HIBERNATE_DDL_AUTO']='none'
  env={e['name']:e.get('value') for e in oldspec['containers'][0].get('env',[])}
  if oldimage==target['image'] and all(env.get(k)==v for k,v in flags.items()):print(json.dumps({'service':name,'alreadyCurrent':True}),flush=True);continue
  if oldimage!=baselines[name]:raise RuntimeError('Unexpected image drift before rollout: '+name)
  previous_pod=ready_pod(before);node=previous_pod['spec']['nodeName'];svc=get('service',name)['spec'];port=svc['ports'][0]['port'];file=manifest(name);original=file.read_text();entry=backup/name;entry.mkdir(mode=0o700);(entry/'deployment-before.json').write_text(json.dumps(before));(entry/file.name).write_text(original)
  s=json.loads(json.dumps(oldspec));s['containers'][0]['image']=target['image'];s['containers'][0]['env']=[e for e in s['containers'][0].get('env',[]) if e['name'] not in flags]+[{'name':k,'value':v} for k,v in sorted(flags.items())];s.setdefault('nodeSelector',{})['kubernetes.io/hostname']=node
  if not s['containers'][0].get('readinessProbe'):s['containers'][0]['readinessProbe']={'tcpSocket':{'port':port},'initialDelaySeconds':20,'periodSeconds':5,'timeoutSeconds':2,'failureThreshold':60}
  patches=[{'op':'test','path':'/metadata/resourceVersion','value':before['metadata']['resourceVersion']},{'op':'replace','path':'/spec/template/spec','value':s}];applied=False;ours=None
  print(json.dumps({'site':a.site,'service':name,'phase':'rolling out','image':target['image'],'node':node,'backup':str(entry)}),flush=True)
  try:
   print(run(K+['patch','deployment',name,'--type=json','-p',json.dumps(patches)]),flush=True);applied=True;ours=get('deployment',name)['spec']['template']
   print(run(K+['rollout','status','deployment/'+name,'--timeout=300s']),flush=True);current=get('deployment',name);pod=ready_pod(current);checks=http_health(name)
   if target.get('jarSha256'):
    actual=run(K+['exec',pod['metadata']['name'],'--','sha256sum',target['jarPath']]).split()[0]
    if actual!=target['jarSha256']:raise RuntimeError('Runtime JAR hash mismatch')
   if get('deployment','integrationservice')['spec']['template']!=integration:raise RuntimeError('Integration template changed during release; investigate')
   persist(file,name,target['image'],flags,s);status=pod['status']['containerStatuses'][0];result={'site':a.site,'service':name,'image':target['image'],'imageID':status.get('imageID'),'pod':pod['metadata']['name'],'ready':True,'health':checks,'configuration':flags,'backup':str(entry),'persistentManifestUpdated':True,'integrationUnchanged':True};results.append(result);(entry/'result.json').write_text(json.dumps(result,indent=2));(backup/'results.json').write_text(json.dumps(results,indent=2));print(json.dumps(result),flush=True)
  except BaseException:
   if applied:
    current=get('deployment',name)
    if ours is not None and current['spec']['template']==ours:
     print('Rolling back '+name,flush=True);print(run(K+['patch','deployment',name,'--type=json','-p',json.dumps([{'op':'replace','path':'/spec/template','value':template}])]),flush=True);print(run(K+['rollout','status','deployment/'+name,'--timeout=300s']),flush=True);file.write_text(original)
   raise
 print(json.dumps({'site':a.site,'completed':len(results),'backup':str(backup),'integrationUnchanged':True}),flush=True)
if __name__=='__main__':main()
