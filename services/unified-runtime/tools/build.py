"""Reproducibly merge site capabilities over exact deployed JARs; never copy private runtime configuration."""
import argparse,hashlib,json,pathlib,subprocess,zipfile,shutil
P=pathlib.Path
p=argparse.ArgumentParser();p.add_argument('--inputs',type=P,required=True);p.add_argument('--compat',type=P,required=True);p.add_argument('--output',type=P,required=True);p.add_argument('--java-home',type=P,required=True);p.add_argument('--asm-core',type=P,required=True);p.add_argument('--asm-tree',type=P,required=True);a=p.parse_args()
repo=P(__file__).resolve().parents[3];out=a.output;out.mkdir(parents=True,exist_ok=True);tools=out/'tools';tools.mkdir(exist_ok=True);jc=str(a.java_home/'bin/javac');java=str(a.java_home/'bin/java');asm=str(a.asm_core)+':'+str(a.asm_tree)
def run(args):subprocess.run([str(x) for x in args],check=True)
run([jc,'--release','17','-cp',asm,'-d',tools,*sorted((P(__file__).parent).glob('*.java'))])
def baseline(site,service):return a.inputs/'baseline'/site/service/'BOOT-INF/classes'
def patch_switch(service,owner,flag,names,helpers=''):
 target=overlay/service/(owner+'.class');current=target if target.exists() else baseline('colton',service)/(owner+'.class')
 run([java,'-cp',str(tools)+':'+asm,'SiteMethodSwitch',current,baseline('fay',service)/(owner+'.class'),flag,target,names,helpers])
def replace(service,owner,fresh,names,original=None):
 target=overlay/service/(owner+'.class');source=original or baseline('colton',service)/(owner+'.class')
 run([java,'-cp',str(tools)+':'+asm,'ReplaceMethods',source,fresh/(owner+'.class'),target,names])
overlay=out/'overlay';changes=json.loads((a.inputs/'jar-comparison.json').read_text())
for row in json.loads((repo/'services/unified-runtime/config/input-artifacts.json').read_text()):
 path=(a.compat/'jars/colton/inboundservice.jar') if row['service']=='inboundservice' else P(changes[row['service']][row['site']+'Jar'])
 if hashlib.sha256(path.read_bytes()).hexdigest()!=row['sha256']:raise RuntimeError('Unverified input JAR: '+row['service'])
inv='com/garyzhangscm/cwms/inventory/';ob='com/garyzhangscm/cwms/outbound/'
patch_switch('inventoryservice',inv+'service/InventoryService','MES_INVENTORY_ATTRIBUTE_LABELS','generateLPNLabel','getLPNLabelContent,setupPrePrintLPNLabelData')
patch_switch('inventoryservice',inv+'service/InventoryActivityService','MES_INVENTORY_ACTIVITY_WAREHOUSE_TIME','lambda$findAll$7f758a3b$1,lambda$findPaginatedInventoryActivities$83626c34$1')
patch_switch('outboundservice',ob+'model/OrderLine','MES_OUTBOUND_MANUAL_PICK_REPORT_V2','setupManualPickableInventoryForDisplay')
legacy_rows=overlay/'outboundservice'/(ob+'model/OrderLine.class')
run([java,'-cp',str(tools)+':'+asm,'NullSafeLegacyManualPick',legacy_rows,legacy_rows])
patch_switch('outboundservice',ob+'service/OrderService','MES_OUTBOUND_MANUAL_PICK_REPORT_V2','setupOrderManualPickReportData')
order_report=overlay/'outboundservice'/(ob+'service/OrderService.class')
run([java,'-cp',str(tools)+':'+asm,'ItemOnlyManualPick',order_report,order_report])
patch_switch('outboundservice',ob+'service/OrderService','MES_OUTBOUND_MANUAL_PICK_REMAINDER','getPickableQuantityForManualPick')
# Compile report-only policy/scoping helpers against production dependencies.
obcp=str(baseline('colton','outboundservice'))+':'+str(a.inputs/'baseline/colton/outboundservice/BOOT-INF/lib/*')
report_sources=repo/'outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/reportfix'
run([jc,'--release','17','-parameters','-sourcepath','','-cp',obcp,'-d',overlay/'outboundservice',*sorted(report_sources.glob('OrderPrint*.java')),report_sources/'ShippingReportScope.java',report_sources/'ManualPickDemandRows.java'])
# Compile the RFID endpoint against the exact unchanged production dependency set.
invcp=str(baseline('colton','inventoryservice'))+':'+str(a.inputs/'baseline/colton/inventoryservice/BOOT-INF/lib/*')
run([jc,'--release','17','-parameters','-sourcepath','','-cp',invcp,'-d',overlay/'inventoryservice',repo/'inventorysvr/src/main/java'/ (inv+'controller/RfidItemLookupController.java'),repo/'inventorysvr/src/main/java'/(inv+'service/RfidItemLookupService.java')])
# Isolate the two small source fixes from unrelated historical changes in full service classes.
for service,module,owner,names in [('inboundservice','inboundsvr','com/garyzhangscm/cwms/inbound/service/ReceiptService','getLPNDocumentContent'),('resourceservice','ressvr','com/garyzhangscm/cwms/resources/MyApplicationRunner','copyReportFile')]:
 if service=='inboundservice':
  jar=a.compat/'jars/colton/inboundservice.jar';base=out/'inbound-baseline';base.mkdir(exist_ok=True)
  with zipfile.ZipFile(jar) as z:
   for n in z.namelist():
    if n.startswith('BOOT-INF/classes/') and n.endswith('.class') or n.startswith('BOOT-INF/lib/') and n.endswith('.jar'):
     f=base/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(z.read(n))
  classes=base/'BOOT-INF/classes';libs=base/'BOOT-INF/lib/*'
 else:classes=baseline('colton',service);libs=a.inputs/'baseline/colton'/service/'BOOT-INF/lib/*'
 fresh=out/'fresh'/service;fresh.mkdir(parents=True,exist_ok=True)
 run([jc,'--release','17','-parameters','-sourcepath','','-cp',str(classes)+':'+str(libs),'-d',fresh,repo/module/'src/main/java'/(owner+'.java')])
 replace(service,owner,fresh,names,classes/(owner+'.class'))
# Preserve every entry except the explicitly selected classes and add the FAY report variants.
report=[]
for service in ['inventoryservice','outboundservice','resourceservice','inboundservice']:
 base=P(changes[service]['coltonJar']) if service!='inboundservice' else a.compat/'jars/colton/inboundservice.jar'
 target=out/(service+'.jar');updates={('BOOT-INF/classes/'+str(f.relative_to(overlay/service))):f.read_bytes() for f in (overlay/service).rglob('*.class')}
 if service=='resourceservice':
  with zipfile.ZipFile(P(changes[service]['fayJar'])) as z:
   for n in changes[service]['changedResources']:
    if not n.startswith('BOOT-INF/classes/reports/meta/order_manual_pick_sheet'):raise RuntimeError('Unexpected resource difference')
    updates[n.replace('/reports/meta/','/reports/site-legacy/')]=(repo/'ressvr/src/main/resources/reports/meta'/P(n).name).read_bytes()
  for f in (repo/'ressvr/src/main/resources/reports/meta').glob('order_manual_pick_sheet*'):
   updates['BOOT-INF/classes/reports/meta/'+f.name]=f.read_bytes()
 with zipfile.ZipFile(base) as src,zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED) as dest:
  for item in src.infolist():dest.writestr(item,updates.pop(item.filename,src.read(item.filename)))
  for name,value in updates.items():dest.writestr(name,value)
 with zipfile.ZipFile(base) as src,zipfile.ZipFile(target) as candidate:
  altered=[n for n in src.namelist() if src.read(n)!=candidate.read(n)];added=sorted(set(candidate.namelist())-set(src.namelist()))
  expected={('BOOT-INF/classes/'+str(f.relative_to(overlay/service))) for f in (overlay/service).rglob('*.class')}
  if service=='resourceservice':expected|={n for n in candidate.namelist() if n.startswith('BOOT-INF/classes/reports/meta/order_manual_pick_sheet')}
  if not set(altered)<=expected:raise RuntimeError('Unselected production code changed')
  if service=='resourceservice':expected|={n for n in candidate.namelist() if n.startswith('BOOT-INF/classes/reports/site-legacy/')}
  if not set(added)<=expected:raise RuntimeError('Unselected entry added')
 row={'service':service,'baseJarSha256':hashlib.sha256(base.read_bytes()).hexdigest(),'candidateJarSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'changedEntries':altered,'addedEntries':added,'allOtherEntriesPreserved':True};report.append(row);print(json.dumps(row),flush=True)
(out/'build-evidence.json').write_text(json.dumps(report,indent=2))
