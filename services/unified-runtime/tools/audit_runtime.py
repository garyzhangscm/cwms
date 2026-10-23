"""Read-only post-release compatibility audit. Outputs no credentials or business rows."""
import argparse,collections,hashlib,json,pathlib,re,subprocess,urllib.request,urllib.error
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--site',choices=['colton','fay'],required=True);p.add_argument('--config',type=P,required=True);a=p.parse_args();K=['kubectl','-n','staging']
def run(args):
    r=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,universal_newlines=True)
    if r.returncode:raise RuntimeError('Audit command failed: '+args[0])
    return r.stdout
def get(kind,name):return json.loads(run(K+['get',kind,name,'-o','json']))
targets=json.loads((a.config/'targets.json').read_text());flags=json.loads((a.config/'sites.json').read_text())[a.site]
names=sorted(set(targets)|{'webclient','workorderservice','qc-shift-report','integrationservice'});rows=[];webpod=None
patterns={'schema':r'(?:column .+ does not exist|relation .+ does not exist|Unknown column|SQLGrammarException|SchemaManagementException)', 'nullPointer':r'NullPointerException','databaseConnection':r'(?:CannotGetJdbcConnectionException|Connection is not available|Communications link failure|Connection refused)', 'networkTimeout':r'(?:SocketTimeoutException|ConnectTimeoutException|ReadTimeoutException)', 'reportCompilation':r'(?:JRException|JRValidationException|JRExpressionEvalException)', 'applicationStartFailure':r'(?:APPLICATION FAILED TO START|BeanCreationException)'}
for name in names:
    d=get('deployment',name);s=d['spec']['template']['spec'];labels=','.join(k+'='+v for k,v in d['spec']['selector']['matchLabels'].items());pods=json.loads(run(K+['get','pods','-l',labels,'-o','json']))['items'];pod=next(x for x in pods if not x['metadata'].get('deletionTimestamp') and x.get('status',{}).get('containerStatuses',[{}])[0].get('ready'));cs=pod['status']['containerStatuses'][0]
    log=run(K+['logs',pod['metadata']['name'],'--since=60m','--tail=12000']);row={'service':name,'ready':cs['ready'],'restartCount':cs['restartCount'],'errorPatternsLast60Minutes':{k:len(re.findall(v,log,re.I)) for k,v in patterns.items()}}
    if name in targets:
        matches=sorted(P('/root/k8s/backups').glob('unified-runtime-*/'+name+'/deployment-before.json'))
        before=json.loads(matches[0].read_text())['spec']['template']['spec'];oldc=before['containers'][0];newc=s['containers'][0];changed=set(flags.get(name,{}))|({'SPRING_JPA_HIBERNATE_DDL_AUTO'} if name!='apigateway' else set())
        oldenv={e['name']:e for e in oldc.get('env',[]) if e['name'] not in changed};newenv={e['name']:e for e in newc.get('env',[]) if e['name'] not in changed}
        row['originalEnvironmentPreserved']=oldenv==newenv
        row['originalVolumesPreserved']=before.get('volumes',[])==s.get('volumes',[])
        row['originalMountsPreserved']=oldc.get('volumeMounts',[])==newc.get('volumeMounts',[])
        row['originalCommandArgsPreserved']=(oldc.get('command'),oldc.get('args'))==(newc.get('command'),newc.get('args'))
        row['originalResourcesPreserved']=oldc.get('resources',{})==newc.get('resources',{})
    if name=='webclient':webpod=pod['metadata']['name']
    rows.append(row)
svc=get('service','webclient')['spec'];base='http://'+svc['clusterIP'];checks=[]
def request(path,method='GET'):
    try:r=urllib.request.urlopen(urllib.request.Request(base+path,method=method),timeout=10)
    except urllib.error.HTTPError as e:r=e
    body=r.read();return r.status,r.headers,body
files=run(K+['exec',webpod,'--','find','/usr/share/nginx/html','-type','f']).splitlines();assets=[x[len('/usr/share/nginx/html'):] for x in files if re.search(r'\.(js|mjs|css|json|woff2?|ttf)$',x)]
if len(assets)>1000:raise RuntimeError('Unexpected asset count')
asset_failures=[]
for path in assets:
    status,headers,body=request(path,'HEAD');ctype=headers.get('Content-Type','');ext=path.rsplit('.',1)[-1]
    valid=status==200 and (('javascript' in ctype) if ext in ('js','mjs') else ('text/css' in ctype) if ext=='css' else ('json' in ctype) if ext=='json' else 'html' not in ctype)
    if not valid:asset_failures.append({'path':path,'status':status,'contentType':ctype})
for path in ['/api/resource/assets/i18n/en-US.json','/api/resource/assets/i18n/zh-CN.json','/api//resource/assets/i18n/en-US.json','/api/resource/assets/file-templates/inventories.csv','/api/qc-shift-report/health','/api/item-import/batches','/api/work-order-import/batches','/api/inventory-handoff/__audit__.json','/api/integration-settings/__audit__.json','/adminer/?__audit__=1']:
    status,headers,body=request(path);row={'path':path,'status':status,'contentType':headers.get('Content-Type',''),'nginxStatic404':status==404 and b'nginx/' in body}
    if '/i18n/' in path:
        try:row['validLanguageObject']=isinstance(json.loads(body),dict)
        except ValueError:row['validLanguageObject']=False
    checks.append(row)
nginx=run(K+['exec',webpod,'--','cat','/etc/nginx/conf.d/default.conf']);locations=re.findall(r'^\s*location\s+([^\n{]+)\s*\{',nginx,re.M)
print(json.dumps({'site':a.site,'services':rows,'staticAssetsChecked':len(assets),'staticAssetFailures':asset_failures,'readOnlyHttpChecks':checks,'nginxLocations':[x.strip() for x in locations],'businessWrites':False,'logWindowLimit':'Last 60 minutes, maximum 12000 lines per pod; counts are diagnostic patterns, not confirmed regressions'}))
