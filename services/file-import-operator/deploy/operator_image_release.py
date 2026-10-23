"""Cut over one existing operator API to its verified image; preserve private state/config."""
import argparse, collections, datetime, hashlib, json, os, pathlib, pwd, shlex, shutil, subprocess, tarfile, tempfile, time, urllib.request, urllib.error
P=pathlib.Path
IMAGE='brianwang1912/mes-import-operator:v20261022-02@sha256:a5ef5b7fcc082e6580ce7f770956c5a97798434f5736b11642066da9c8f149b0'
def run(args):
    return subprocess.check_output(args,universal_newlines=True,stderr=subprocess.STDOUT)
def active(unit):
    return subprocess.call(['systemctl','is-active','--quiet',unit])==0
def job_counts(config):
    counts=collections.Counter()
    for p in (P(config['stateDirectory'])/'operator-jobs').glob('*.json'):
        counts[json.loads(p.read_text()).get('state','UNKNOWN')]+=1
    return dict(counts)
def idle(config):
    counts=job_counts(config)
    if any(counts.get(x,0) for x in ['QUEUED','RUNNING','UNKNOWN']):raise RuntimeError('Operator has pending/active jobs; do not cut over')
    return counts
def request(url):
    try:
        with urllib.request.urlopen(url,timeout=5) as r:return r.status,json.loads(r.read().decode())
    except urllib.error.HTTPError as e:return e.code,json.loads(e.read().decode())
def health(config,kind):
    host=config['operatorApi']['bind'];host='127.0.0.1' if host=='0.0.0.0' else host
    base='http://'+host+':'+str(config['operatorApi']['port'])
    for attempt in range(20):
        try:
            status,body=request(base+'/healthz')
            if status==200 and body.get('result')==0:break
        except Exception:pass
        time.sleep(.5)
    else:raise RuntimeError('Operator health did not become ready')
    status,body=request(base+('/item-import' if kind=='items' else '/work-order-import')+'/batches')
    if config.get('unconfiguredReason'):
        if status!=503:raise RuntimeError('Unconfigured source accepted a business request')
    elif status not in [401,403]:raise RuntimeError('Anonymous request was not rejected')
    return {'health':200,'anonymousStatus':status}
def args_for(config,kind,container,uid,gid,config_file,state_override=None):
    args=['/usr/bin/docker','run','--rm','--name',container,'--network','host','--read-only','--user',str(uid)+':'+str(gid),'--cap-drop','ALL','--security-opt','no-new-privileges','--pids-limit','128','--tmpfs','/tmp:rw,noexec,nosuid,size=16m']
    # Scoped equivalent of the original host process access; never relabel shared state.
    if shutil.which('getenforce') and run(['getenforce']).strip()=='Enforcing':args+=['--security-opt','label=disable']
    parents=set([] if state_override else [str(config_file.parent)])
    for key in ['usernameFile','passwordFile']:
        value=config.get('ftp',{}).get(key)
        if value:parents.add(str(P(value).parent))
    for path in sorted(parents):args+=['-v',path+':'+path+':ro']
    if state_override:
        args+=['-v',str(state_override)+':/state:rw','-v',str(config_file)+':/run/import/config.json:ro']
    else:
        state_root=P(config['stateDirectory']).parent
        args+=['-v',str(state_root)+':'+str(state_root)+':rw']
    args+=[IMAGE,'--kind',kind,'--config','/run/import/config.json' if state_override else str(config_file)]
    if config.get('unconfiguredReason'):args+=['--unconfigured-reason',config['unconfiguredReason']]
    return args

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--kind',choices=['items','work-orders'],required=True);parser.add_argument('--site',choices=['colton','fay'],default='colton');parser.add_argument('--unconfigured',action='store_true');args=parser.parse_args();kind=args.kind
    if args.site=='fay':
        name='cwms-fay-item-operator' if kind=='items' else 'cwms-fay-work-order-operator'
        user=pwd.getpwnam('cwmsfayitem');config_file=P('/etc')/name/'config.json'
        if not config_file.exists():
            if kind=='work-orders' and not args.unconfigured:raise RuntimeError('FAY has no configured Work Order source')
            if kind=='items':
                config=json.loads(P('/etc/cwms-fay-oracle-items/config.json').read_text())
                if config['oracleItems'].get('filenamePrefix')!='fayint_item' or config.get('deleteSourceFiles') is not False:raise RuntimeError('Unexpected FAY prefix/retention policy')
                if not config['oracleItems'].get('inventoryBaseUrl'):config['oracleItems']['inventoryBaseUrl']=os.environ['MES_INVENTORY_BASE_URL']
            else:
                config={'stateDirectory':'/var/lib/cwms-fay-work-order-operator/state','unconfiguredReason':'Work Order file import is not configured for FAY. Import retry is unavailable.'}
                P(config['stateDirectory']).mkdir(parents=True,exist_ok=True)
            config['operatorApi']={'authBaseUrl':os.environ['MES_AUTH_BASE_URL'],'userBaseUrl':os.environ['MES_USER_BASE_URL'],'layoutBaseUrl':os.environ['MES_LAYOUT_BASE_URL'],'bind':os.environ['MES_OPERATOR_BIND_ADDRESS'],'port':18793 if kind=='items' else 18792}
            if os.environ['MES_FORBIDDEN_SITE_PREFIX'] in json.dumps(config):raise RuntimeError('Cross-factory target in FAY configuration')
            config_file.parent.mkdir(mode=0o750,parents=True,exist_ok=True);os.chown(str(config_file.parent),0,user.pw_gid);config_file.write_text(json.dumps(config,indent=2));os.chown(str(config_file),0,user.pw_gid);os.chmod(str(config_file),0o640)
        timer_name='cwms-fay-oracle-items' if kind=='items' else 'cwms-fay-oracle-work-orders'
    else:
        name='cwms-item-operator' if kind=='items' else 'cwms-work-order-operator';user=pwd.getpwnam('cwmsitem');config_file=P('/etc')/name/'config.json';timer_name='cwms-oracle-items' if kind=='items' else 'cwms-oracle-work-orders'
    unit=name+'.service';uid,gid=user.pw_uid,user.pw_gid;config=json.loads(config_file.read_text());before_jobs=idle(config)
    if active(timer_name+'.service'):raise RuntimeError('Daily importer is active; wait until it finishes')
    timer_before=active(timer_name+'.timer')
    P('/root/cwms-import-backups').mkdir(mode=0o700,exist_ok=True)
    backup=P('/root/cwms-import-backups')/('image-'+name+'-'+datetime.datetime.utcnow().strftime('%Y%m%dT%H%M%SZ'));backup.mkdir(mode=0o700)
    unit_location=run(['systemctl','show',unit,'--property=FragmentPath','--value']).strip();unit_path=P(unit_location) if unit_location else P('/etc/systemd/system')/unit;original=unit_path.read_bytes() if unit_path.exists() else None
    if original is not None:(backup/'original.service').write_bytes(original)
    print('Pulling verified operator image',flush=True);print(run(['docker','pull',IMAGE]),flush=True)
    # Exercise the actual image with an empty private queue; never run a parallel worker on the live queue.
    qa=backup/'qa';qa.mkdir(mode=0o755);qa_state=qa/'state';qa_state.mkdir();os.chown(str(qa_state),uid,gid);qa_config=dict(config);qa_config['operatorApi']=dict(config['operatorApi'],bind='127.0.0.1',port=19893 if kind=='items' else 19892);qa_config['stateDirectory']='/state';qa_file=qa/'config.json';qa_file.write_text(json.dumps(qa_config));os.chown(str(qa_file),uid,gid);os.chmod(str(qa_file),0o400)
    qa_name=name+'-release-check';qa_args=args_for(qa_config,kind,qa_name,uid,gid,qa_file,qa_state);qa_args.remove('--rm');qa_args.insert(2,'-d')
    try:
        run(qa_args)
        try:health(qa_config,kind)
        except BaseException:
            print(run(['docker','logs','--tail','25',qa_name]),flush=True)
            raise
    finally:subprocess.call(['docker','rm','-f',qa_name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    idle(config);was_active=active(unit)
    if not was_active and args.site!='fay':raise RuntimeError('Existing operator is not active; investigate before replacement')
    if was_active:subprocess.check_call(['systemctl','stop',unit])
    try:
        idle(config)
        state_root=P(config['stateDirectory']).parent
        with tarfile.open(str(backup/'state-before.tar.gz'),'w:gz') as archive:archive.add(str(state_root),arcname=state_root.name)
        command=args_for(config,kind,name,uid,gid,config_file)
        text='[Unit]\nDescription=Image-based '+name+'\nAfter=network-online.target docker.service\nRequires=docker.service\n\n[Service]\nType=simple\nExecStartPre=-/usr/bin/docker rm -f '+name+'\nExecStart='+ ' '.join(shlex.quote(x) for x in command)+'\nExecStop=/usr/bin/docker stop --time 20 '+name+'\nRestart=on-failure\nRestartSec=5\nTimeoutStopSec=35\n\n[Install]\nWantedBy=multi-user.target\n'
        unit_path.write_text(text);subprocess.check_call(['systemctl','daemon-reload']);subprocess.check_call(['systemctl','start',unit]);checks=health(config,kind)
        if original is None:subprocess.check_call(['systemctl','enable',unit])
        actual=json.loads(run(['docker','inspect',name]))[0]
        if actual['Config']['Image']!=IMAGE or not actual['State']['Running']:raise RuntimeError('Wrong runtime image')
        if active(timer_name+'.timer')!=timer_before:raise RuntimeError('Import timer status changed')
        result={'service':unit,'image':IMAGE,'backup':str(backup),'uid':uid,'gid':gid,'health':checks,'jobStatesBefore':before_jobs,'timerUnchanged':True,'businessWritesForVerification':False}
        (backup/'result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result),flush=True)
    except BaseException:
        subprocess.call(['systemctl','stop',unit]);subprocess.call(['docker','rm','-f',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL);
        if original is not None:unit_path.write_bytes(original)
        else:
            subprocess.call(['systemctl','disable',unit]);unit_path.unlink()
        subprocess.call(['systemctl','daemon-reload'])
        if was_active:subprocess.call(['systemctl','start',unit])
        raise

if __name__=='__main__':main()
