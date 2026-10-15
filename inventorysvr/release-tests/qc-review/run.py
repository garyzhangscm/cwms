#!/usr/bin/env python3
"""Offline checks against an existing inventory Spring Boot JAR: run.py /path/to/inventory.jar"""
import os, pathlib, shutil, subprocess, sys, tempfile, zipfile
here=pathlib.Path(__file__).resolve().parent
module=here.parents[1]
java_home=os.environ.get('JAVA_HOME')
def executable(name):
    value=str(pathlib.Path(java_home)/'bin'/name) if java_home else shutil.which(name)
    if not value: raise SystemExit('Set JAVA_HOME to JDK 17 or add JDK 17 to PATH')
    return value
with tempfile.TemporaryDirectory(prefix='qc-review-regression-') as tmp:
    tmp=pathlib.Path(tmp)
    with zipfile.ZipFile(sys.argv[1]) as jar:
        for name in jar.namelist():
            if name.startswith(('BOOT-INF/classes/', 'BOOT-INF/lib/')): jar.extract(name,tmp)
    cp=os.pathsep.join([str(tmp/'patched'),str(tmp/'BOOT-INF/classes'),str(tmp/'BOOT-INF/lib/*')])
    base=module/'src/main/java/com/garyzhangscm/cwms/inventory'
    source=[base/'model/QCInspectionRequestItemOption.java',base/'service/QCFailureReviewPolicy.java',base/'service/QCFailureReviewService.java',base/'service/QCReviewPreparationService.java',base/'controller/QCFailureReviewController.java']+list(here.glob('*.java'))
    subprocess.run([executable('javac'),'-parameters','-cp',cp,'-d',str(tmp/'patched')]+[str(p) for p in source],check=True)
    for test in ['QCFailureReviewServiceRegression','QCPreparationRegression']:
        subprocess.run([executable('java'),'-Xverify:all','-cp',cp,test],check=True)
