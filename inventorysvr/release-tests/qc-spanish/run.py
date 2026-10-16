#!/usr/bin/env python3
"""Offline QC Spanish compatibility regression: run.py /path/to/inventory.jar"""
import os, pathlib, shutil, subprocess, sys, tempfile, zipfile
here = pathlib.Path(__file__).resolve().parent
module = here.parents[1]
java_home = os.environ.get('JAVA_HOME')
def executable(name):
    result = str(pathlib.Path(java_home) / 'bin' / name) if java_home else shutil.which(name)
    if not result: raise SystemExit('Set JAVA_HOME to JDK 17+ or add it to PATH')
    return result
with tempfile.TemporaryDirectory(prefix='qc-spanish-regression-') as temp:
    temp = pathlib.Path(temp)
    with zipfile.ZipFile(sys.argv[1]) as jar:
        for name in jar.namelist():
            if name.startswith(('BOOT-INF/classes/', 'BOOT-INF/lib/')): jar.extract(name, temp)
    cp = os.pathsep.join([str(temp/'patched'), str(temp/'BOOT-INF/classes'), str(temp/'BOOT-INF/lib/*')])
    base = module/'src/main/java/com/garyzhangscm/cwms/inventory'
    source = [base/'model/QCRuleItem.java', base/'service/QCRuleService.java', here/'QCSpanishRegression.java']
    subprocess.run([executable('javac'), '-cp', cp, '-d', str(temp/'patched'), *map(str, source)], check=True)
    subprocess.run([executable('java'), '-Xverify:all', '-cp', cp, 'QCSpanishRegression'], check=True)
