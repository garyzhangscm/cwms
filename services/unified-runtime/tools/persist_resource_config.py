"""Persist the existing Resource configuration only; no Kubernetes mutation or restart."""
import argparse,datetime,hashlib,json,pathlib,socket,subprocess,yaml
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--site',choices=['colton','fay'],required=True);a=p.parse_args()
if socket.gethostname()!=('k8s-master-ar' if a.site=='fay' else 'k8s-master'):raise RuntimeError('Wrong factory master')
K=['kubectl','-n','staging'];name='resourceservice'
def live():return json.loads(subprocess.check_output(K+['get','deployment',name,'-o','json'],universal_newlines=True))
d=live();s=d['spec']['template']['spec'];c=s['containers'][0];path=P('/root/k8s/5-backend-app.yaml');original=path.read_text();docs=list(yaml.safe_load_all(original));matches=[x for x in docs if x and x.get('kind')=='Deployment' and x.get('metadata',{}).get('name')==name]
if len(matches)!=1:raise RuntimeError('Resource manifest not unique')
target=matches[0]['spec']['template']['spec'];tc=target['containers'][0]
if tc['image']!=c['image']:raise RuntimeError('Resource image drift')
tc['env']=c.get('env',[]);tc['volumeMounts']=c.get('volumeMounts',[]);target['volumes']=s.get('volumes',[])
updated=yaml.safe_dump_all(docs);list(yaml.safe_load_all(updated))
if live()['metadata']['resourceVersion']!=d['metadata']['resourceVersion']:raise RuntimeError('Concurrent deployment change')
b=P('/root/k8s/backups')/('resource-manifest-'+datetime.datetime.utcnow().strftime('%Y%m%dT%H%M%SZ'));b.mkdir(mode=0o700);(b/path.name).write_text(original);path.write_text(updated)
result={'site':a.site,'service':name,'existingEnvironmentAndMountsPersisted':True,'liveResourcesChanged':False,'businessWrites':False,'backup':str(b)};(b/'result.json').write_text(json.dumps(result));print(json.dumps(result))
