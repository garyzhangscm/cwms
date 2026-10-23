import pathlib,argparse,json,subprocess
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--build',type=P,required=True);a=p.parse_args();root=P(__file__).resolve().parents[3];sites=json.loads((root/'services/unified-runtime/config/sites.json').read_text());images=json.loads((a.build/'published-images.json').read_text());results=[];docker='/Applications/Docker.app/Contents/Resources/bin/docker'
for row in images:
 service=row['service'];folder=a.build/'checks'/service;kind={'inventoryservice':'inventory','outboundservice':'outbound','resourceservice':'resource','inboundservice':'inbound'}[service];test='InboundLabelCompatibility' if kind=='inbound' else 'UnifiedReadOnlyChecks'
 for site in ['colton','fay']:
  args=[docker,'run','--rm','--platform','linux/amd64','--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--pids-limit','128','--tmpfs','/tmp:rw,size=128m','-v',str(folder)+':/qa:ro','--entrypoint','java',row['image'],'-Xverify:all',*[f'-D{k}={v}' for k,v in sites[site][service].items()],'-cp','/qa/tests:/qa/BOOT-INF/classes:/qa/BOOT-INF/lib/*',test]+([] if kind=='inbound' else [kind])
  r=subprocess.run(args,capture_output=True,text=True);(folder/(site+'-image.log')).write_text(r.stdout+r.stderr);result={'service':service,'site':site,'image':row['image'],'exit':r.returncode,'results':[s for s in r.stdout.splitlines() if s.startswith('RESULT')],'error':r.stderr[-1500:] if r.returncode else ''};results.append(result);print(json.dumps(result),flush=True)
(a.build/'image-regression-results.json').write_text(json.dumps(results,indent=2))
if any(r['exit'] or any('ERROR=' in s for s in r['results']) for r in results):raise SystemExit(1)
