"""Compile the isolated overlay and run H2 checks; never accesses a business server."""
import argparse, os, subprocess, tempfile, zipfile
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--backend',required=True,help='Existing outboundsvr checkout (read only)')
p.add_argument('--jar',required=True,help='Existing Spring Boot outbound JAR')
p.add_argument('--h2',required=True,help='Local H2 dependency JAR')
p.add_argument('--java-home',default=os.environ.get('JAVA_HOME'),required=not bool(os.environ.get('JAVA_HOME')))
a=p.parse_args();base=Path(__file__).resolve().parent
with tempfile.TemporaryDirectory(prefix='allocation-check-') as temporary:
 build=Path(temporary)
 with zipfile.ZipFile(a.jar) as jar:
  for name in jar.namelist():
   if name.startswith(('BOOT-INF/classes/','BOOT-INF/lib/')) and not name.endswith('/'):
    target=build/name
    if not target.resolve().is_relative_to(build.resolve()):raise ValueError('Unsafe JAR member')
    target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(jar.read(name))
 subprocess.run(['python3',str(base/'prepare-source.py'),'--backend',a.backend,'--output',str(build/'overlay')],check=True)
 cp=os.pathsep.join([str(build/'classes'),str(build/'BOOT-INF/classes'),str(build/'BOOT-INF/lib/*'),str(Path(a.h2).resolve())])
 sources=list((build/'overlay/src/main/java').rglob('*.java'))+[base/'AllocationTaskCheck.java']
 subprocess.run([str(Path(a.java_home)/'bin/javac'),'--release','13','-encoding','UTF-8','-cp',cp,'-d',str(build/'classes')]+list(map(str,sources)),check=True)
 result=subprocess.run([str(Path(a.java_home)/'bin/java'),'-cp',cp,'AllocationTaskCheck',str(base/'schema.sql')],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 if result.returncode:print(result.stdout)
 else:print(result.stdout.strip().splitlines()[-1])
 raise SystemExit(result.returncode)
