"""Reproduce Fay candidate from locally available, verified image JARs."""
import hashlib,json,pathlib,sys,zipfile
base,current,out=map(pathlib.Path,sys.argv[1:])
assert hashlib.sha256(base.read_bytes()).hexdigest()=='49e198d8dce2ca756f70edb675d1a71ec2ebeeed696d3c86f43dd5fce6925be7'
assert hashlib.sha256(current.read_bytes()).hexdigest()=='5cbf0fe413a2b1a32f255a11dfa0ae428c7481a15fc5fe13927f69f85c35a28b'
keep=['BOOT-INF/classes/com/garyzhangscm/cwms/outbound/'+n for n in ['service/OrderService.class','reportfix/ManualPickDemandRows.class','reportfix/PalletBreakdown.class']]
with zipfile.ZipFile(base) as a,zipfile.ZipFile(current) as b,zipfile.ZipFile(out,'w') as z:
 for i in b.infolist():z.writestr(i,a.read(i.filename) if i.filename in keep else b.read(i.filename))
 for n in keep:
  if n not in b.namelist():z.writestr(a.getinfo(n),a.read(n))
assert hashlib.sha256(out.read_bytes()).hexdigest()=='18d627d846a83bf0aa494a3c7dc39bf1604afaa32d56ad0e23f0d64c62d4a03c'
print('Fay candidate reproduced with existing delete/report fixes preserved')
