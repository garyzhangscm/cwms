from pathlib import Path
import zipfile,hashlib,json
r=Path('/private/tmp/pick-team-20261028');old=Path('/private/tmp/lpn-query-reuse-20261028/outbound-lpn-query-reuse.jar')
assert hashlib.sha256(old.read_bytes()).hexdigest()=='d0cec975f9ee08248426d40f81fe967156b3cd4240875aec062814934c63efbf'
n='BOOT-INF/classes/com/garyzhangscm/cwms/outbound/service/ManualPickQueueService.class';data=(r/'classes'/n.removeprefix('BOOT-INF/classes/')).read_bytes();out=r/'outbound-pick-team.jar'
with zipfile.ZipFile(old) as a,zipfile.ZipFile(out,'w') as b:
 for i in a.infolist():b.writestr(i,data if i.filename==n else a.read(i.filename))
with zipfile.ZipFile(old) as a,zipfile.ZipFile(out) as b:
 assert [x for x in a.namelist() if a.read(x)!=b.read(x)]==[n]
(r/'jar-build.json').write_text(json.dumps(dict(candidateSha256=hashlib.sha256(out.read_bytes()).hexdigest(),baselineSha256=hashlib.sha256(old.read_bytes()).hexdigest(),changed=[n])))
(r/'images.json').write_text(json.dumps({'outbound':'brianwang1912/cwms-outboundserver:v1.62-pick-team-20261028-01'}))
(r/'Dockerfile').write_text('FROM brianwang1912/cwms-outboundserver:v1.62-lpn-query-reuse-20261028-01\nCOPY outbound-pick-team.jar /usr/local/service/outboundserver-v1.62.jar\n')
for file in ['deploy.py','preload.py']:
 s=(Path('/private/tmp/lpn-query-reuse-20261028')/file).read_text().replace('lpn-query-reuse-20261028','pick-team-20261028').replace('v1.62-manual-pick-queue-20261028-04','v1.62-lpn-query-reuse-20261028-01').replace('testWebUnchanged=False','testWebUnchanged=True')
 if file=='deploy.py':
  s=s.replace(" assert dbstate()==initial", " team=read(outbound+'/orders/7137/manual-pick-queue?warehouseId=1')[0];assert team and all('submittedBy' in row for row in team),'Submitter missing in queue response'\n assert dbstate()==initial")
 (r/file).write_text(s)
print((r/'jar-build.json').read_text())
