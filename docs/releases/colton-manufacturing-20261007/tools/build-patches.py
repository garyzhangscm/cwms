from pathlib import Path
import zipfile,subprocess,json,hashlib
b=Path('/Users/xiangxiangwang/Documents/ChatGPT/MES APP');r=Path('/private/tmp/colton-manufacturing-release-20261007');jdk=next((b/'.local/tools').glob('jdk-17*/Contents/Home/bin/javac'))
changes={'layout':['model/WarehouseConfiguration.java','service/WarehouseConfigurationService.java'], 'outbound':['model/WarehouseConfiguration.java','clients/WarehouseLayoutServiceRestemplateClient.java','repository/PickRepository.java','service/ManufacturingIssuePolicyService.java','service/PickService.java']}
manifest={}
for kind,module in [('layout','layoutserver'),('outbound','outboundsvr')]:
 before=r/f'colton-{kind}-before.jar';base=r/f'{kind}-baseline';base.mkdir(exist_ok=True)
 with zipfile.ZipFile(before) as z:
  for n in z.namelist():
   if n.startswith('BOOT-INF/classes/') or (n.startswith('BOOT-INF/lib/') and n.endswith('.jar')):
    if n.endswith('/'):continue
    dest=base/n;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(n))
 classes=r/f'{kind}-patch-classes';classes.mkdir(exist_ok=True)
 sources=[str(b/module/'src/main/java/com/garyzhangscm/cwms'/kind/p) for p in changes[kind]]
 subprocess.run([str(jdk),'-parameters','-g','-cp',str(base/'BOOT-INF/classes')+':'+str(base/'BOOT-INF/lib/*'),'-d',str(classes),*sources],check=True)
 patches={f'BOOT-INF/classes/{p.relative_to(classes)}':p.read_bytes() for p in classes.rglob('*.class')}
 after=r/f'{kind}-release.jar'
 with zipfile.ZipFile(before) as zin,zipfile.ZipFile(after,'w') as zout:
  original=set(zin.namelist())
  for info in zin.infolist():zout.writestr(info,patches.get(info.filename,zin.read(info.filename)))
  for n,data in patches.items():
   if n not in original:zout.writestr(n,data,compress_type=zipfile.ZIP_DEFLATED)
 with zipfile.ZipFile(before) as a,zipfile.ZipFile(after) as c:
  for n in a.namelist():
   if n not in patches:assert a.read(n)==c.read(n),n
 manifest[kind]={'baselineSha256':hashlib.sha256(before.read_bytes()).hexdigest(),'releaseSha256':hashlib.sha256(after.read_bytes()).hexdigest(),'classesChanged':sorted(patches),'sourceFiles':changes[kind]}
 print(kind, 'compiled against formal baseline, only',len(patches),'classes replaced')
(r/'patch-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
