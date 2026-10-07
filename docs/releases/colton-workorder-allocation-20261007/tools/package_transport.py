from pathlib import Path
import subprocess,zipfile,json,hashlib
root=Path('/Users/xiangxiangwang/Documents/ChatGPT/MES APP');out=Path('/private/tmp/colton-allocate-wo390-20261007');ctx=out/'transport-image';ctx.mkdir(exist_ok=True)
java=root/'.local/tools/jdk-17.0.20.1+1/Contents/Home/bin';cp=':'.join(str(root/'.local/m2/repository/org/ow2/asm'/n/'9.5'/(n+'-9.5.jar')) for n in ['asm','asm-tree'])
subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(out),str(out/'PatchWorkOrderAllocationTransport.java')],check=True)
base=out/'image/workorderserver-v1.62.jar';assert hashlib.sha256(base.read_bytes()).hexdigest()=='23afb60bfc7ea0c6f27f734fb94db310508d9a9746844e1b1d5c54eb35389f79'
subprocess.run([str(java/'java'),'-cp',str(out)+':'+cp,'PatchWorkOrderAllocationTransport',str(base),str(root/'workordersvr/target/classes'),str(out)],check=True)
replacements={'BOOT-INF/classes/com/garyzhangscm/cwms/workorder/'+n:(out/f).read_bytes() for n,f in [('clients/OutboundServiceRestemplateClient.class','OutboundServiceRestemplateClient.class'),('clients/InventoryServiceRestemplateClient.class','InventoryServiceRestemplateClient.class'),('model/Item.class','Item.class')]}
name='BOOT-INF/classes/com/garyzhangscm/cwms/workorder/service/WorkOrderAllocationDataService.class';replacements[name]=(root/'workordersvr/target/classes'/name.removeprefix('BOOT-INF/classes/')).read_bytes();jar=ctx/'workorderserver-v1.62.jar'
with zipfile.ZipFile(base) as old,zipfile.ZipFile(jar,'w') as new:
 for info in old.infolist():new.writestr(info,replacements.get(info.filename,old.read(info.filename)))
with zipfile.ZipFile(base) as old,zipfile.ZipFile(jar) as new:
 changed=[n for n in old.namelist() if old.read(n)!=new.read(n)];assert set(changed)==set(replacements);assert set(new.namelist())==set(old.namelist())
(out/'transport-jar-manifest.json').write_text(json.dumps({'changed':changed,'baseline_sha256':hashlib.sha256(base.read_bytes()).hexdigest(),'output_sha256':hashlib.sha256(jar.read_bytes()).hexdigest()},indent=2))
print('Verified: only allocation transport/reference classes changed; dependencies preserved.')
