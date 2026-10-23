"""Build report-only candidates on the current deployed JARs without losing prior fixes."""
import argparse, hashlib, json, pathlib, subprocess, zipfile
P = pathlib.Path
p = argparse.ArgumentParser()
for name in ('outbound', 'resource', 'output', 'java_home', 'asm_core', 'asm_tree'):
    p.add_argument('--' + name.replace('_', '-'), type=P, required=True)
a = p.parse_args()
repo = P(__file__).resolve().parents[3]
out = a.output
out.mkdir(parents=True, exist_ok=True)
java, javac = (a.java_home / 'bin' / n for n in ('java', 'javac'))
asm = str(a.asm_core) + ':' + str(a.asm_tree)
def run(*args):
    subprocess.run([str(x) for x in args], check=True)
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
assert digest(a.outbound) == '8388605177fdf4b6faa89b7881cbaecd6790086c8a9129d55bce163066a81f7b', 'Unexpected outbound baseline'
assert digest(a.resource) == 'fe33f4bf76c9d8dad630444310166d6da0c7514a36c230aa73c590590c67e629', 'Unexpected resource baseline'
checks = []
for service, source in [('outboundservice', a.outbound), ('resourceservice', a.resource)]:
    root = out / service
    with zipfile.ZipFile(source) as z:
        for n in z.namelist():
            if n.startswith(('BOOT-INF/classes/', 'BOOT-INF/lib/')) and n.endswith(('.class', '.jar')):
                f = root / n
                f.parent.mkdir(parents=True, exist_ok=True)
                f.write_bytes(z.read(n))
    classes = root / 'BOOT-INF/classes'
    updates = {}
    if service == 'outboundservice':
        tools = out / 'tools'
        tools.mkdir(exist_ok=True)
        run(javac, '--release', '17', '-cp', asm, '-d', tools, P(__file__).with_name('ItemOnlyManualPick.java'))
        name = 'com/garyzhangscm/cwms/outbound/service/OrderService.class'
        patched = out / 'patched' / name
        run(java, '-cp', str(tools) + ':' + asm, 'ItemOnlyManualPick', classes / name, patched)
        updates['BOOT-INF/classes/' + name] = patched.read_bytes()
        fresh = out / 'policy-classes'
        run(javac, '--release', '17', '-parameters', '-sourcepath', '', '-cp', str(classes) + ':' + str(root / 'BOOT-INF/lib/*'), '-d', fresh, *sorted((repo / 'outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/reportfix').glob('OrderPrint*.java')), repo / 'outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/reportfix/ShippingReportScope.java', repo / 'outboundsvr/src/main/java/com/garyzhangscm/cwms/outbound/reportfix/ManualPickDemandRows.java')
        for f in fresh.rglob('*.class'):
            updates['BOOT-INF/classes/' + str(f.relative_to(fresh))] = f.read_bytes()
    else:
        run(javac, '--release', '17', '-parameters', '-sourcepath', '', '-cp', str(classes) + ':' + str(root / 'BOOT-INF/lib/*'), '-d', out / 'resource-patched', repo / 'ressvr/src/main/java/com/garyzhangscm/cwms/resources/MyApplicationRunner.java')
        name = 'com/garyzhangscm/cwms/resources/MyApplicationRunner.class'
        run(javac, '--release', '17', '-cp', asm, '-d', out / 'tools', P(__file__).with_name('ReplaceMethods.java'))
        run(java, '-cp', str(out / 'tools') + ':' + asm, 'ReplaceMethods', classes / name, out / 'resource-patched' / name, out / 'resource-selected' / name, 'copyReportFile')
        updates['BOOT-INF/classes/' + name] = (out / 'resource-selected' / name).read_bytes()
        for f in (repo / 'ressvr/src/main/resources/reports/meta').glob('order_manual_pick_sheet*'):
            for folder in ('meta', 'site-legacy'):
                updates['BOOT-INF/classes/reports/' + folder + '/' + f.name] = f.read_bytes()
    target = out / (service + '.jar')
    allowed = set(updates)
    with zipfile.ZipFile(source) as z, zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED) as dest:
        for item in z.infolist():
            dest.writestr(item, updates.pop(item.filename, z.read(item.filename)))
        for name, value in updates.items():
            dest.writestr(name, value)
    with zipfile.ZipFile(source) as old, zipfile.ZipFile(target) as new:
        changed = [n for n in old.namelist() if old.read(n) != new.read(n)]
        added = sorted(set(new.namelist()) - set(old.namelist()))
        assert set(changed + added) <= allowed
        assert set(old.namelist()) <= set(new.namelist())
    checks.append(dict(service=service, baselineSha256=digest(source), candidateSha256=digest(target), changed=changed, added=added, allOtherEntriesPreserved=True))
(out / 'build-evidence.json').write_text(json.dumps(checks, indent=2))
print(json.dumps(checks, indent=2))
