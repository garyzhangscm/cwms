import argparse,json,pathlib,subprocess,concurrent.futures,re,shutil
P=pathlib.Path;p=argparse.ArgumentParser();p.add_argument('--build',type=P,required=True);a=p.parse_args();docker='/Applications/Docker.app/Contents/Resources/bin/docker';specs={
 'inventoryservice':('cwms-inventoryserver','brianwang1912/cwms-inventoryserver:v1.62-colton-preprinted-lpn-20261016@sha256:a74eebf6bda0ed0837f8947e01941e830b97e1251dec2f756e64b1d8104582af','inventoryserver-v1.62.jar'),
 'outboundservice':('cwms-outboundserver','brianwang1912/cwms-outboundserver:v1.62-fay-preprinted-lpn-20261016@sha256:bc20da87e0196d96a3eabdfae4f00cee8b4f52d69571f76b278b1a8dce756c5a','outboundserver-v1.62.jar'),
 'resourceservice':('cwms-ressvr','brianwang1912/cwms-ressvr:v1.62-colton-preprinted-lpn-20261016@sha256:5e84d3adc8d3188cd7fd2f7e747803c11fa3b1d990d5457e24ee7fb36744d33f','resourceserver-v1.62.jar'),
 'inboundservice':('cwms-inboundserver','garyzhangscm/cwms-inboundserver:v1.62@sha256:658900aeda5b4a441f77d78b12951f65b2b76dedf5b04db3dffc73f62ca302b4','inboundserver-v1.62.jar')}
checks=json.loads((a.build/'regression-results.json').read_text());assert len(checks)==8 and all(r['exit']==0 for r in checks)
def job(item):
 service,(repo,base,path)=item;folder=a.build/'images'/service;folder.mkdir(parents=True,exist_ok=True);shutil.copy2(a.build/(service+'.jar'),folder/'service.jar');(folder/'Dockerfile').write_text('FROM '+base+'\nCOPY service.jar /usr/local/service/'+path+'\n');image='brianwang1912/'+repo+':v1.62-unified-20261022-01'
 for action,args in [('build',[docker,'build','--platform','linux/amd64','-t',image,str(folder)]),('push',[docker,'push',image])]:
  result=subprocess.run(args,capture_output=True,text=True);(folder/(action+'.log')).write_text(result.stdout+result.stderr)
  if result.returncode:raise RuntimeError(service+' '+action+' failed: '+(result.stdout+result.stderr)[-1000:])
  if action=='push':digest=re.search(r'digest: (sha256:[a-f0-9]{64})',result.stdout+result.stderr).group(1)
 row={'service':service,'image':image+'@'+digest,'base':base};print(json.dumps(row),flush=True);return row
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:results=list(ex.map(job,specs.items()))
(a.build/'published-images.json').write_text(json.dumps(results,indent=2))
