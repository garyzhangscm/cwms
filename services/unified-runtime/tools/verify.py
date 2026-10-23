"""Read-only final image/configuration checks; no transaction or business import is invoked."""
import json,pathlib,subprocess,urllib.request,urllib.error,argparse,yaml
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--site',choices=['colton','fay'],required=True);p.add_argument('--config',type=P,required=True);a=p.parse_args();K=['kubectl','-n','staging'];targets=json.loads((a.config/'targets.json').read_text());sites=json.loads((a.config/'sites.json').read_text())[a.site];baselines={r['name']:r['image'] for r in json.loads((a.config/'baselines.json').read_text())[a.site]};hashes=json.loads((a.config/'report-hashes.json').read_text())[a.site]
def run(args):
 p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,universal_newlines=True)
 if p.returncode:raise RuntimeError('Read-only verification command failed')
 return p.stdout
get=lambda kind,name:json.loads(run(K+['get',kind,name,'-o','json']))
rows=[];podmap={}
for name in sorted(set(targets)|{'webclient','workorderservice','qc-shift-report','integrationservice'}):
 d=get('deployment',name);c=d['spec']['template']['spec']['containers'][0];expected=targets[name]['image'] if name in targets else baselines[name]
 if c['image']!=expected:raise RuntimeError('Wrong desired image: '+name)
 desired=d['spec']['replicas'];ready=d.get('status',{}).get('readyReplicas',0)
 if ready!=desired or ready!=1:raise RuntimeError('Service not ready: '+name)
 labels=','.join(k+'='+v for k,v in d['spec']['selector']['matchLabels'].items());pods=json.loads(run(K+['get','pods','-l',labels,'-o','json']))['items'];pod=next(p for p in pods if not p['metadata'].get('deletionTimestamp') and p['spec']['containers'][0]['image']==expected and p.get('status',{}).get('containerStatuses',[{}])[0].get('ready'));podmap[name]=pod['metadata']['name'];runtime=pod['status']['containerStatuses'][0]
 env={e['name']:e.get('value') for e in c.get('env',[])};flags=sites.get(name,{})
 if any(env.get(k)!=v for k,v in flags.items()):raise RuntimeError('Wrong capability flags: '+name)
 rows.append({'service':name,'image':expected,'imageID':runtime.get('imageID'),'ready':ready,'node':pod['spec']['nodeName'],'configuration':{k:env[k] for k in flags},'integrationExcluded':name=='integrationservice'})
# Verify actual selected report files; a pre-existing customized template can take precedence.
cm=get('configmap','app-properties');config=yaml.safe_load(cm['data']['application.yml']);report=config.get('report',{});folder=report.get('template',{}).get('folder');custom=report.get('customizedTemplate',{}).get('folder');templates=[]
if not folder:raise RuntimeError('Cannot resolve report template folder')
for file,expected in hashes.items():
 actual=run(K+['exec',podmap['resourceservice'],'--','sha256sum',str(P(folder)/file)]).split()[0];custom_match=False
 if actual!=expected and custom:
  probe=subprocess.run(K+['exec',podmap['resourceservice'],'--','sha256sum',str(P(custom)/file)],stdout=subprocess.PIPE,stderr=subprocess.PIPE,universal_newlines=True)
  custom_match=probe.returncode==0 and probe.stdout.split()[0]==actual
 if actual!=expected and not custom_match:raise RuntimeError('Report template does not match selected or customized version: '+file)
 templates.append({'file':file,'sha256':actual,'selectedVersionMatches':actual==expected,'existingCustomizationMatches':custom_match})
s=get('service','inventoryservice')['spec'];url='http://'+s['clusterIP']+':'+str(s['ports'][0]['port'])+'/items-query/rfid-upcs?companyId=1&warehouseId=1&upcs=INVALID'
try:r=urllib.request.urlopen(url,timeout=8)
except urllib.error.HTTPError as e:r=e
body=json.loads(r.read());message=str(body.get('message',''));rfid={'enabled':a.site=='fay','invalidUpcRejected':'Invalid UPC/GTIN' in message,'routeAbsent':'No static resource' in message}
if a.site=='fay' and not rfid['invalidUpcRejected'] or a.site=='colton' and not rfid['routeAbsent']:raise RuntimeError('RFID route did not follow site configuration')
results=[]
for backup in sorted(P('/root/k8s/backups').glob('unified-runtime-*')):
 result=backup/'results.json'
 if result.exists():results.extend(json.loads(result.read_text()))
print(json.dumps({'site':a.site,'services':rows,'reportTemplates':templates,'rfidRoute':rfid,'deployments':results,'integrationImageUnchanged':True,'businessWritesForVerification':False}))
