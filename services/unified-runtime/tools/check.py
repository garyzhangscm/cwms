import pathlib,argparse,zipfile,subprocess,json,os
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--build',type=P,required=True);p.add_argument('--inputs',type=P,required=True);p.add_argument('--java-home',type=P,required=True);p.add_argument('--service');a=p.parse_args();root=P(__file__).resolve().parents[3];out=a.build/'checks';out.mkdir(exist_ok=True);java=str(a.java_home/'bin/java');javac=str(a.java_home/'bin/javac');tests=root/'services/unified-runtime/tests';results=[]
for service,kind in [('inventoryservice','inventory'),('outboundservice','outbound'),('resourceservice','resource'),('inboundservice','inbound')]:
 if a.service and a.service!=service:continue
 folder=out/service;folder.mkdir(exist_ok=True)
 with zipfile.ZipFile(a.build/(service+'.jar')) as z:
  for n in z.namelist():
   if n.startswith('BOOT-INF/classes/') and (n.endswith('.class') or '/reports/' in n) or n.startswith('BOOT-INF/lib/') and n.endswith('.jar'):
    if n.endswith('/'):continue
    f=folder/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(z.read(n))
 cp=str(folder/'BOOT-INF/classes')+':'+str(folder/'BOOT-INF/lib/*');compiled=folder/'tests';compiled.mkdir(exist_ok=True);test='InboundLabelCompatibility' if kind=='inbound' else 'UnifiedReadOnlyChecks'
 subprocess.run([javac,'--release','17','-cp',cp,'-d',str(compiled),str(tests/(test+'.java'))],check=True,capture_output=True)
 for site in ['colton','fay']:
  flags=json.loads((root/'services/unified-runtime/config/sites.json').read_text())[site][service];command=[java,'-Xverify:all',*[f'-D{k}={v}' for k,v in flags.items()],'-cp',str(compiled)+':'+cp,test]+([] if kind=='inbound' else [kind]);r=subprocess.run(command,capture_output=True,text=True);log=folder/(site+'.log');log.write_text(r.stdout+r.stderr);lines=[s for s in r.stdout.splitlines() if s.startswith('RESULT')];row={'service':service,'site':site,'exit':r.returncode,'results':lines,'errors':r.stderr[-2000:] if r.returncode else ''};results.append(row);print(json.dumps(row),flush=True)
if a.service and (a.build/'regression-results.json').exists():results=[v for v in json.loads((a.build/'regression-results.json').read_text()) if v['service']!=a.service]+results
(a.build/'regression-results.json').write_text(json.dumps(results,indent=2))
if any(r['exit'] for r in results):raise SystemExit(1)
if any('ERROR=' in line for r in results for line in r['results']):raise SystemExit('A label fixture still fails')
