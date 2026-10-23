"""Compare persistent image/configuration references with live resources; print no values."""
import json,pathlib,subprocess,yaml
K=['kubectl','-n','staging'];P=pathlib.Path
def get(kind,name):return json.loads(subprocess.check_output(K+['get',kind,name,'-o','json'],universal_newlines=True))
rows=[];maps=[]
for filename in ['3-framework.yaml','4-frontend-app.yaml','5-backend-app.yaml','6-qc-shift-report.yaml']:
    path=P('/root/k8s')/filename
    if not path.exists():continue
    for d in yaml.safe_load_all(path.read_text()):
        if not d:continue
        name=d.get('metadata',{}).get('name')
        if d.get('kind')=='ConfigMap' and 'default.conf.template' in d.get('data',{}):maps.append({'name':name,'dataMatchesLive':d['data']==get('configmap',name)['data']})
        if d.get('kind')!='Deployment' or name=='integrationservice':continue
        live=get('deployment',name);s=d['spec']['template']['spec'];t=live['spec']['template']['spec'];c=s['containers'][0];lc=t['containers'][0]
        env=lambda x:{e['name']:e for e in x.get('env',[])}
        row={'service':name,'manifest':filename,'imageMatchesLive':c['image']==lc['image'],'environmentMatchesLive':env(c)==env(lc),'volumeSourcesMatchLive':{v['name']:v.get('configMap',{}).get('name') or v.get('persistentVolumeClaim',{}).get('claimName') or v.get('hostPath',{}).get('path') for v in s.get('volumes',[])}=={v['name']:v.get('configMap',{}).get('name') or v.get('persistentVolumeClaim',{}).get('claimName') or v.get('hostPath',{}).get('path') for v in t.get('volumes',[])}}
        rows.append(row)
print(json.dumps({'deployments':rows,'webConfigMaps':maps,'businessWrites':False}))
