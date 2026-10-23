"""Repair aggregate removal on the verified FAY pallet-sheet production JAR."""
import argparse,hashlib,json,pathlib,subprocess,zipfile
P=pathlib.Path
p=argparse.ArgumentParser();p.add_argument('--baseline',type=P,required=True);p.add_argument('--output',type=P,required=True);p.add_argument('--java-home',type=P,required=True);p.add_argument('--asm-core',type=P,required=True);p.add_argument('--asm-tree',type=P,required=True);a=p.parse_args()
expected='24e5f599cf928e5b6357601aa146a2d3a2a5292a974e5f3c87d6294ef5e5765f'
if hashlib.sha256(a.baseline.read_bytes()).hexdigest()!=expected:raise RuntimeError('Unexpected outbound baseline; stop before building')
repo=P(__file__).resolve().parents[3];out=a.output;out.mkdir(parents=True,exist_ok=True)
classes=out/'baseline/BOOT-INF/classes';libs=out/'baseline/BOOT-INF/lib';fresh=out/'fresh';merged=out/'merged';tools=out/'tools';tests=out/'tests'
for folder in [classes,libs,fresh,merged,tools,tests]:folder.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(a.baseline) as z:
 for name in z.namelist():
  if name.endswith('/'):continue
  if (name.startswith('BOOT-INF/classes/') and name.endswith('.class')) or (name.startswith('BOOT-INF/lib/') and name.endswith('.jar')):
   target=out/'baseline'/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(name))
cp=str(classes)+':'+str(libs/'*');asm=str(a.asm_core)+':'+str(a.asm_tree);java=a.java_home/'bin/java';javac=a.java_home/'bin/javac'
def run(args):subprocess.run([str(x) for x in args],check=True)
owner='com/garyzhangscm/cwms/outbound/service/OrderService.class'
run([javac,'--release','17','-cp',asm,'-d',tools,P(__file__).parent/'ReplaceMethods.java'])
run([javac,'--release','17','-parameters','-sourcepath','','-cp',cp,'-d',fresh,repo/'outboundsvr/src/main/java'/owner.replace('.class','.java')])
run([java,'-cp',str(tools)+':'+asm,'ReplaceMethods',classes/owner,fresh/owner,merged/owner,'removeOrder'])
target=out/'outboundservice.jar';entry='BOOT-INF/classes/'+owner
with zipfile.ZipFile(a.baseline) as src,zipfile.ZipFile(target,'w') as dst:
 for info in src.infolist():dst.writestr(info,(merged/owner).read_bytes() if info.filename==entry else src.read(info.filename))
with zipfile.ZipFile(a.baseline) as src,zipfile.ZipFile(target) as dst:
 if src.namelist()!=dst.namelist():raise RuntimeError('JAR entry set changed')
 changed=[name for name in src.namelist() if src.read(name)!=dst.read(name)]
 if changed!=[entry]:raise RuntimeError('Unselected entry changed')
evidence={'baselineSha256':expected,'candidateSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'changedEntries':changed,'entrySetPreserved':True,'businessWrites':False}
(out/'build-evidence.json').write_text(json.dumps(evidence,indent=2));print(json.dumps(evidence),flush=True)
