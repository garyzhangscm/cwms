"""Fetch only published pinned build artifacts; no server access or credentials required."""
import argparse,pathlib,json,subprocess,hashlib,zipfile,shutil
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--output',type=P,required=True);a=p.parse_args();repo=P(__file__).resolve().parents[3];docker=shutil.which('docker') or '/Applications/Docker.app/Contents/Resources/bin/docker';inputs=json.loads((repo/'services/unified-runtime/config/input-artifacts.json').read_text());a.output.mkdir(parents=True,exist_ok=True)
for row in inputs:
 site,name=row['site'],row['service'];folder=a.output/('compat/jars/colton' if name=='inboundservice' else 'jars/'+site);folder.mkdir(parents=True,exist_ok=True);jar=folder/(name+'.jar')
 if not jar.exists():
  pull=subprocess.run([docker,'pull','--platform','linux/amd64',row['image']],capture_output=True,text=True)
  if pull.returncode:raise RuntimeError('Could not fetch pinned build input '+name)
  container=subprocess.check_output([docker,'create','--platform','linux/amd64','--network','none','--entrypoint','/bin/true',row['image']],text=True).strip()
  try:subprocess.run([docker,'cp',container+':'+row['jarPath'],str(jar)],capture_output=True,check=True)
  finally:subprocess.run([docker,'rm',container],capture_output=True,check=True)
 if hashlib.sha256(jar.read_bytes()).hexdigest()!=row['sha256']:raise RuntimeError('Input artifact hash mismatch: '+site+'/'+name)
 if name!='inboundservice':
  with zipfile.ZipFile(jar) as z:
   for path in z.namelist():
    if path.startswith('BOOT-INF/classes/') and path.endswith('.class') or path.startswith('BOOT-INF/lib/') and path.endswith('.jar'):
     dest=a.output/'baseline'/site/name/path;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(path))
 print('Verified '+site+'/'+name,flush=True)
rows={}
for name in ['inventoryservice','outboundservice','resourceservice']:
 colton=a.output/'jars/colton'/(name+'.jar');fay=a.output/'jars/fay'/(name+'.jar');resources=[]
 if name=='resourceservice':
  with zipfile.ZipFile(colton) as x,zipfile.ZipFile(fay) as y:resources=[n for n in x.namelist() if n in y.namelist() and n.startswith('BOOT-INF/classes/reports/') and x.read(n)!=y.read(n)]
 rows[name]={'coltonJar':str(colton.resolve()),'fayJar':str(fay.resolve()),'changedResources':resources}
(a.output/'jar-comparison.json').write_text(json.dumps(rows,indent=2))
