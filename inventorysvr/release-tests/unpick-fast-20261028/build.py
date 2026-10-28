from pathlib import Path
import zipfile,hashlib,json
r=Path('/private/tmp/unpick-fast-20261028');base=Path('/private/tmp/manual-pick-pipeline-20261027/inventory-pipeline.jar');assert hashlib.sha256(base.read_bytes()).hexdigest()=='34b791f8deb23ef98bf12b0e0fb490ab1d4c9b9d1468404a8f3ea65909c51049'
names=['com/garyzhangscm/cwms/inventory/service/InventoryService.class','com/garyzhangscm/cwms/inventory/service/InventoryConsolidationService.class'];patch={'BOOT-INF/classes/'+n:(r/'classes'/n).read_bytes() for n in names}
output=r/'inventory-unpick-fast.jar'
with zipfile.ZipFile(base) as a,zipfile.ZipFile(output,'w') as b:
 for info in a.infolist():b.writestr(info,patch.get(info.filename,a.read(info.filename)))
with zipfile.ZipFile(base) as a,zipfile.ZipFile(output) as b:
 changes=[n for n in a.namelist() if a.read(n)!=b.read(n)];assert set(changes)==set(patch)
(r/'jar-build.json').write_text(json.dumps(dict(baselineSha256=hashlib.sha256(base.read_bytes()).hexdigest(),candidateSha256=hashlib.sha256(output.read_bytes()).hexdigest(),changed=changes),indent=2))
(r/'images.json').write_text(json.dumps({'inventory':'brianwang1912/cwms-inventoryserver:v1.62-unpick-fast-20261028-01'}))
(r/'Dockerfile').write_text('FROM brianwang1912/cwms-inventoryserver:v1.62-manual-pipeline-20261027-01@sha256:0aa39bcb9acd4aabb0e5cf0b1913f2b9f251a788b927df716133b793897fce76\nCOPY inventory-unpick-fast.jar /usr/local/service/inventoryserver-v1.62.jar\n')
s=(Path('/private/tmp/lpn-query-reuse-20261028/deploy.py')).read_text().replace('/tmp/lpn-query-reuse-20261028','/tmp/unpick-fast-20261028')
s=s.replace("expected={'outboundservice':'brianwang1912/cwms-outboundserver:v1.62-manual-pick-queue-20261028-04'}","expected={'inventoryservice':'brianwang1912/cwms-inventoryserver:v1.62-manual-pipeline-20261027-01@sha256:0aa39bcb9acd4aabb0e5cf0b1913f2b9f251a788b927df716133b793897fce76'}")
start=s.index(" env=container.setdefault('env',[])");end=s.index(" secrets=template['spec']",start);s=s[:start]+s[end:]
s=s.replace("changed.append('outboundservice');templates['outboundservice']=install('outboundservice','outbound',before['outboundservice']['spec']['template'])","changed.append('inventoryservice');templates['inventoryservice']=install('inventoryservice','inventory',before['inventoryservice']['spec']['template'])")
s=s.replace("oldweb=ready('webclient')","oldweb=ready('webclient');oldoutbound=get('deployment','outboundservice')['spec']")
s=s.replace("assert get('deployment','webclient')['spec']==web,'Formal web deployment changed'","assert get('deployment','webclient')['spec']==web,'Formal web deployment changed'\n assert get('deployment','outboundservice')['spec']==oldoutbound,'Outbound deployment changed'")
s=s.replace('testWebUnchanged=False','testWebUnchanged=True').replace("'manual-pick-queue-'+datetime","'unpick-fast-'+datetime")
(r/'deploy.py').write_text(s)
s=Path('/private/tmp/lpn-query-reuse-20261028/preload.py').read_text().replace('lpn-query-reuse-20261028','unpick-fast-20261028').replace("['outbound']","['inventory']");(r/'preload.py').write_text(s)
print((r/'jar-build.json').read_text())
