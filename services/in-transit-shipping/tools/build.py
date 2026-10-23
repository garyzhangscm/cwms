#!/usr/bin/env python3
"""Build against the exact deployed JAR, preserving all unmodified entries and methods."""
import argparse, hashlib, json, os, struct, subprocess, zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--java-home',required=True);args=p.parse_args()
module=Path(__file__).resolve().parents[1];root=module.parent.parent;build=module/'build';baseline=build/'baseline.jar'
expected='15da73e384fa4ba0cd1ff94f7c44e9a7548c01c6cffe22646936fc07047dbbcc'
assert hashlib.sha256(baseline.read_bytes()).hexdigest()==expected,'Baseline differs from deployed FAY image; stop and verify'
classes=build/'baseline-classes';libs=build/'libs';patched=build/'patched';tools=build/'tools'
for path in (classes,libs,patched,tools):path.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(baseline) as z:
 for name in z.namelist():
  if name.endswith('/'):continue
  target=None
  if name.startswith('BOOT-INF/classes/'):target=classes/name[len('BOOT-INF/classes/'):]
  if name.startswith('BOOT-INF/lib/'):target=libs/Path(name).name
  if target:
   target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(name))
cp=str(classes)+os.pathsep+str(libs/'*');java=Path(args.java_home)/'bin/java';javac=Path(args.java_home)/'bin/javac'
sources=sorted((root/'outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/transit').glob('*.java'))
subprocess.run([str(javac),'--release','13','-encoding','UTF-8','-cp',cp,'-d',str(patched),*map(str,sources)],check=True)
subprocess.run([str(javac),'--release','13','-cp',cp,'-d',str(tools),str(module/'tools/PatchTransitDestination.java')],check=True)
name='com/garyzhangscm/cwms/outbound/service/ShipmentService.class'
subprocess.run([str(java),'-cp',str(tools)+os.pathsep+cp,'PatchTransitDestination',str(classes/name),str(patched/name)],check=True)
# Parse method Code bytes, requiring every pre-existing method except the routing hook to be byte-identical.
def method_codes(data):
 pos=8
 def u2():
  nonlocal pos
  n=int.from_bytes(data[pos:pos+2],'big');pos+=2;return n
 def u4():
  nonlocal pos
  n=int.from_bytes(data[pos:pos+4],'big');pos+=4;return n
 count=u2();pool={};i=1
 while i<count:
  tag=data[pos];pos+=1
  if tag==1:
   n=u2();pool[i]=data[pos:pos+n].decode('utf-8',errors='replace');pos+=n
  elif tag in (3,4):pos+=4
  elif tag in (5,6):pos+=8;i+=1
  elif tag in (7,8,16,19,20):pos+=2
  elif tag in (9,10,11,12,17,18):pos+=4
  elif tag==15:pos+=3
  else:raise ValueError('unknown constant pool tag')
  i+=1
 pos+=6;interfaces=u2();pos+=2*interfaces
 for _ in range(u2()):
  pos+=6
  for _ in range(u2()):u2();size=u4();pos+=size
 methods={}
 for _ in range(u2()):
  u2();name=pool[u2()];desc=pool[u2()];code=None
  for _ in range(u2()):
   attr=pool[u2()];size=u4();start=pos
   if attr=='Code':
    u2();locals_=u2();length=u4();instructions=data[pos:pos+length];pos+=length
    exceptions=u2();handlers=data[pos:pos+8*exceptions]
    code=(locals_,instructions,handlers)
   pos=start+size
  methods[name+desc]=code
 return methods
before=method_codes((classes/name).read_bytes());after=method_codes((patched/name).read_bytes())
assert before.keys()==after.keys(),'Existing method set changed'
changed_methods=[k for k in before if before[k]!=after[k]]
assert len(changed_methods)==1 and changed_methods[0].startswith('completeShipmentByOrder('),changed_methods
out=build/'outboundservice.jar';changed=[];new=[]
updates={'BOOT-INF/classes/'+str(f.relative_to(patched)):f.read_bytes() for f in patched.rglob('*.class')}
with zipfile.ZipFile(baseline) as src,zipfile.ZipFile(out,'w') as dst:
 names=set(src.namelist())
 for info in src.infolist():
  content=updates.pop(info.filename,src.read(info.filename))
  if content!=src.read(info.filename):changed.append(info.filename)
  dst.writestr(info,content)
 for path,content in updates.items():dst.writestr(path,content,compress_type=zipfile.ZIP_DEFLATED);new.append(path)
with zipfile.ZipFile(baseline) as a,zipfile.ZipFile(out) as b:
 assert changed==['BOOT-INF/classes/'+name],changed
 for path in a.namelist():
  if path not in changed:assert a.read(path)==b.read(path),path
manifest={'baselineSha256':expected,'releaseSha256':hashlib.sha256(out.read_bytes()).hexdigest(),'changedExistingEntries':changed,'changedExistingMethods':changed_methods,'addedEntries':sorted(new),'allOtherEntriesAndMethodCodePreserved':True}
(build/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');print(json.dumps(manifest,indent=2))

# Load the patched class first, then exercise the actual shipping destination hook.
regression_cp=str(patched)+os.pathsep+cp
subprocess.run([str(javac),'--release','13','-cp',regression_cp,'-d',str(tools),*map(str,sorted((module/'tests').glob('*.java')))],check=True)
subprocess.run([str(java),'-cp',str(tools)+os.pathsep+regression_cp,'TransitRegression'],check=True)
subprocess.run([str(java),'-cp',str(tools)+os.pathsep+regression_cp,'TransitAccessRegression'],check=True)

subprocess.run([str(java),'-cp',str(tools)+os.pathsep+regression_cp,'TransitShipmentFlow'],check=True)
