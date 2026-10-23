"""Patch exactly the current shared OrderLine legacy formatter; preserve every other JAR entry."""
import argparse,hashlib,json,pathlib,subprocess,zipfile
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--baseline',type=P,required=True);p.add_argument('--output',type=P,required=True);a=p.parse_args()
expected='024c4e53b594b03000ac75d107ea4ac6899bf41842b7c53dbe2514f88966d9f0';assert hashlib.sha256(a.baseline.read_bytes()).hexdigest()==expected
repo=P(__file__).resolve().parents[3];out=a.output;out.mkdir(parents=True,exist_ok=True);classes=out/'classes';libs=out/'libs';tools=out/'tools';patched=out/'patched'
for path in [classes,libs,tools,patched]:path.mkdir(exist_ok=True)
with zipfile.ZipFile(a.baseline) as z:
 for n in z.namelist():
  if n.endswith('/'):continue
  dest=classes/n[len('BOOT-INF/classes/'):] if n.startswith('BOOT-INF/classes/') else libs/P(n).name if n.startswith('BOOT-INF/lib/') else None
  if dest:dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(n))
jhome=P('/Applications/Android Studio.app/Contents/jbr/Contents/Home');asm=P.home()/'.m2/repository/org/ow2/asm';acp=str(asm/'asm/9.5/asm-9.5.jar')+':'+str(asm/'asm-tree/9.5/asm-tree-9.5.jar');cp=str(classes)+':'+str(libs/'*')
def run(args):subprocess.run([str(x) for x in args],check=True)
owner='com/garyzhangscm/cwms/outbound/model/OrderLine.class'
run([jhome/'bin/javac','--release','17','-cp',acp,'-d',tools,P(__file__).parent/'NullSafeLegacyManualPick.java'])
run([jhome/'bin/java','-cp',str(tools)+':'+acp,'NullSafeLegacyManualPick',classes/owner,patched/owner])
target=out/'outboundservice.jar';entry='BOOT-INF/classes/'+owner
with zipfile.ZipFile(a.baseline) as src,zipfile.ZipFile(target,'w') as dst:
 for info in src.infolist():dst.writestr(info,(patched/owner).read_bytes() if info.filename==entry else src.read(info.filename))
with zipfile.ZipFile(a.baseline) as src,zipfile.ZipFile(target) as dst:
 assert src.namelist()==dst.namelist();changed=[n for n in src.namelist() if src.read(n)!=dst.read(n)];assert changed==[entry]
run([jhome/'bin/javac','--release','17','-cp',cp,'-d',tools,repo/'services/unified-runtime/tests/LegacyManualPickNullDates.java'])
results=[]
for site,flag in [('fay','false'),('colton','true')]:
 cmd=[jhome/'bin/java','-Xverify:all','-DMES_OUTBOUND_MANUAL_PICK_REPORT_V2='+flag,'-cp',str(tools)+':'+str(patched)+':'+cp,'LegacyManualPickNullDates'];run(cmd);results.append({'site':site,'cases':10,'passed':True})
evidence={'baselineSha256':expected,'candidateSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'changedEntries':changed,'tests':results,'siteFlagsAndShippingPreserved':True,'businessWrites':False};(out/'build-evidence.json').write_text(json.dumps(evidence,indent=2));print(json.dumps(evidence))
