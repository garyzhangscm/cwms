#!/usr/bin/env python3
"""Compile merged source against existing service dependencies and run offline checks.
No deployment, application configuration, production database or captured records are used.
"""
import argparse, json, os, subprocess, tempfile, zipfile
from pathlib import Path
p = argparse.ArgumentParser()
for name in ['inventory-jar','outbound-jar','resources-jar','java-home','test-libs']:
    p.add_argument('--'+name, required=True)
a=p.parse_args(); here=Path(__file__).resolve().parent; root=here.parent.parent
java=Path(a.java_home)/'bin/java'; javac=java.with_name('javac')
libs=json.loads(Path(a.test_libs).read_text())
files={
 'inventorysvr':['allocationwire/CandidateWireCodec.java','inventory/controller/CompactCandidateController.java','inventory/controller/ManualPickInventoryController.java','inventory/model/ManualPickInventoryOperation.java','inventory/repository/ManualPickInventoryOperationRepository.java','inventory/service/ManualPickInventoryService.java'],
 'outboundsvr':['allocationwire/CandidateWireCodec.java','outbound/clients/CompactCandidateReadAspect.java','outbound/exception/ExceptionResponse.java','outbound/model/OrderLine.java','outbound/reportfix/ManualPickReportFailureAspect.java','outbound/reportfix/ManualPickReportRows.java','outbound/service/OrderService.java','outbound/service/PickService.java','outbound/service/PreprintedManualPickService.java','outbound/model/ManualPickOperation.java','outbound/repository/ManualPickOperationRepository.java','outbound/controller/PreprintedManualPickController.java'],
 'ressvr':['resources/model/ReportType.java','resources/model/PreprintedLpn.java','resources/repository/PreprintedLpnRepository.java','resources/controller/PreprintedLpnController.java']}
tests={'inventorysvr':['ProductionProducerCheck'],'outboundsvr':['ProductionAspectCheck','ReportCheck','SimpleReportCheck'],'ressvr':[]}
with tempfile.TemporaryDirectory(prefix='v164-source-check-') as temporary:
    work=Path(temporary); paths={}
    rows=[]
    for i in range(500):
        rows.append({'id':i+1,'lpn':'SYNTHETIC-'+str(i),'quantity':20,'locationId':i%7+1,
            'createdTime':'2026-01-01T10:00:00Z','item':{'id':1,'name':'SYNTHETIC-ITEM','description':'Test metadata '*150,'allowAllocationByLPN':False,'allocationRoundUpStrategyType':'NONE'},
            'inventoryStatus':{'id':1,'name':'Available'},'itemPackageType':{'id':1,'name':'TEST-PACK','itemUnitOfMeasures':[{'id':1,'quantity':1,'length':1.0,'width':1.0,'height':1.0,'weight':1.0,'unitOfMeasure':{'id':1,'name':'PCS'}}]}})
    legacy=work/'legacy.json'; legacy.write_text(json.dumps(rows)); compact=work/'compact.json'
    for module,jar in [('inventorysvr',a.inventory_jar),('outboundsvr',a.outbound_jar),('ressvr',a.resources_jar)]:
        target=work/module
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                if n.startswith(('BOOT-INF/classes/','BOOT-INF/lib/')) and n.endswith(('.jar','.class')):
                    dest=target/n
                    if not dest.resolve().is_relative_to(target.resolve()): raise ValueError('Invalid JAR entry')
                    dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(n))
        cp=os.pathsep.join([str(target/'merged'),str(target/'tests'),*libs,str(target/'BOOT-INF/classes'),str(target/'BOOT-INF/lib/*')]); paths[module]=cp
        sources=[root/module/'src/main/java/com/garyzhangscm/cwms'/n for n in files[module]]
        subprocess.run([str(javac),'--release','13' if module=='outboundsvr' else '17','-parameters','-encoding','UTF-8','-cp',cp,'-d',str(target/'merged'),*map(str,sources)],check=True)
        test_sources=[here/(n+'.java') for n in tests[module]]
        regression=list((root/module/'release-tests').glob('*Regression.java')); test_sources+=regression
        subprocess.run([str(javac),'--release','17','-cp',cp,'-d',str(target/'tests'),*map(str,test_sources)],check=True)
        ext=target/'tests/mockito-extensions'; ext.mkdir(parents=True,exist_ok=True); (ext/'org.mockito.plugins.MockMaker').write_text('mock-maker-subclass')
        policy=['-Djava.security.manager=default','-Djava.security.policy=='+str(here/'offline-test.policy')]
        for name in tests[module]+[x.stem for x in regression]:
            args=[str(legacy),str(compact)] if name in ['ProductionProducerCheck','ProductionAspectCheck'] else []
            if name=='PreprintedLpnReportRegression': args=[str(root/module/'src/main/resources/reports/meta/preprinted_lpn_4x6_203dpi.prn')]
            subprocess.run([str(java),'-Xmx768m','-Xverify:all',*policy,'-cp',cp,name,*args],check=True)
    template=root/'ressvr/src/main/resources/reports/meta'
    cp=os.pathsep.join([str(template),str(work/'render'),paths['ressvr']])
    subprocess.run([str(javac),'--release','17','-cp',cp,'-d',str(work/'render'),str(here/'RenderSheet.java')],check=True)
    subprocess.run([str(java),'-Djava.awt.headless=true','-cp',cp,'RenderSheet',str(template),str(work/'pdf')],check=True)
print('PASS merged V1.64 source compilation and all offline regressions; no production writes.')
