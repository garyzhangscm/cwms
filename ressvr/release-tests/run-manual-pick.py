#!/usr/bin/env python3
"""Run offline regressions against an existing service JAR: run-manual-pick.py /path/to/service.jar"""
import os, pathlib, shutil, subprocess, sys, tempfile, zipfile
here=pathlib.Path(__file__).resolve().parent
module=here.parent
java_home=os.environ.get('JAVA_HOME')
def exe(name):
 result=str(pathlib.Path(java_home)/'bin'/name) if java_home else shutil.which(name)
 if not result: raise SystemExit('Set JAVA_HOME to JDK 17+ or put javac and java on PATH')
 return result
with tempfile.TemporaryDirectory(prefix='manual-pick-regression-') as tmp:
 tmp=pathlib.Path(tmp)
 with zipfile.ZipFile(sys.argv[1]) as jar:
  for name in jar.namelist():
   if name.startswith(('BOOT-INF/classes/','BOOT-INF/lib/')): jar.extract(name,tmp)
 cp=os.pathsep.join(map(str,[tmp/'patched',tmp/'BOOT-INF/classes',tmp/'BOOT-INF/lib/*']))
 base=module/'src/main/java/com/garyzhangscm/cwms/resources'
 files=['model/ReportType.java', 'model/PreprintedLpn.java', 'repository/PreprintedLpnRepository.java', 'controller/PreprintedLpnController.java']
 sources=[base/file for file in files]+[here/'PreprintedLpnReportRegression.java']
 subprocess.run([exe('javac'),'--release','17','-parameters','-cp',cp,'-d',str(tmp/'patched'),*map(str,sources)],check=True)
 subprocess.run([exe('java'),'-Xverify:all','-cp',cp,'PreprintedLpnReportRegression',*[str(module/'src/main/resources/reports/meta/preprinted_lpn_4x6_203dpi.prn')]],check=True)
