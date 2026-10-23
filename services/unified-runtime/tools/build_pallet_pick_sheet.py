"""Patch only advisory pallet rows and Manual Pick Sheet templates on verified live artifacts."""
import argparse,hashlib,json,pathlib,subprocess,zipfile
P=pathlib.Path;p=argparse.ArgumentParser()
for k in ['outbound','resource','output','java-home']:p.add_argument('--'+k,type=P,required=True)
a=p.parse_args();repo=P(__file__).resolve().parents[3];a.output.mkdir(parents=True,exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(a.outbound)=='ba852feab5d47609e8dc137b8ecd60f232dec912b63f2f67cf7d6074716bfe8e'
assert sha(a.resource)=='e035c5cc8cd395202b743272b326dccb6b9afecc8941d6997a4735c204c68977'
evidence=[]
for name,src in [('outboundservice',a.outbound),('resourceservice',a.resource)]:
 updates={};root=a.output/name
 with zipfile.ZipFile(src) as z:
  for n in z.namelist():
   if n.startswith('BOOT-INF/classes/') and n.endswith('.class') or n.startswith('BOOT-INF/lib/') and n.endswith('.jar'):
    f=root/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(z.read(n))
 if name=='outboundservice':
  fresh=a.output/'classes';base=repo/'outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/reportfix'
  subprocess.run([str(a.java_home/'bin/javac'),'--release','17','-parameters','-sourcepath','','-cp',str(root/'BOOT-INF/classes')+':'+str(root/'BOOT-INF/lib/*'),'-d',str(fresh),str(base/'ManualPickDemandRows.java'),str(base/'PalletBreakdown.java')],check=True)
  for f in fresh.rglob('*.class'):updates['BOOT-INF/classes/'+str(f.relative_to(fresh))]=f.read_bytes()
 else:
  for f in (repo/'ressvr/src/main/resources/reports/meta').glob('order_manual_pick_sheet*'):
   for folder in ['meta','site-legacy']:updates['BOOT-INF/classes/reports/'+folder+'/'+f.name]=f.read_bytes()
 allowed=set(updates);target=a.output/(name+'.jar')
 with zipfile.ZipFile(src) as old,zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED) as new:
  for n in old.infolist():new.writestr(n,updates.pop(n.filename,old.read(n.filename)))
  for n,b in updates.items():
   entry=zipfile.ZipInfo(n,date_time=(1980,1,1,0,0,0));entry.compress_type=zipfile.ZIP_DEFLATED;new.writestr(entry,b)
 with zipfile.ZipFile(src) as old,zipfile.ZipFile(target) as new:
  changed=[n for n in old.namelist() if old.read(n)!=new.read(n)];added=sorted(set(new.namelist())-set(old.namelist()));assert set(changed+added)<=allowed
 evidence.append({'service':name,'baselineSha256':sha(src),'candidateSha256':sha(target),'changed':changed,'added':added,'allOtherEntriesPreserved':True})
(a.output/'build-evidence.json').write_text(json.dumps(evidence,indent=2)+'\n');print(json.dumps(evidence,indent=2))
