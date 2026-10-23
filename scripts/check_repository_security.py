#!/usr/bin/env python3
"""Report repository paths and line numbers only; never echo a suspected secret."""
import argparse,ipaddress,pathlib,re,subprocess,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--ref');args=parser.parse_args()
ip_pattern=re.compile(r'(?<![\w.])(?:\d{1,3}\.){3}\d{1,3}(?![\w.])')
token_pattern=re.compile(r'(?<![\w-])(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[A-Z0-9]{16}|sk-(?:proj-)?[A-Za-z0-9_-]{40,}|xox[baprs]-[A-Za-z0-9-]{20,})(?![\w-])')
key_pattern=re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----')
assignment=re.compile(r'(?i)^\s*(?:#\s*)?([\w.-]*(?:password|passwd|secret|token|encrypt[_\.]?key)[\w.-]*)\s*[:=]\s*(.*)$')
def private_host(value):
 try:address=ipaddress.ip_address(value)
 except ValueError:return False
 octets=value.split('.');return octets[0]=='10' or octets[:2]==['192','168'] or octets[0]=='172' and 16<=int(octets[1])<=31
if args.ref:
 names=subprocess.check_output(['git','ls-tree','-rz','--name-only',args.ref],cwd=ROOT).decode().split('\0')
else:
 names=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode().split('\0')
findings=[]
for name in filter(None,names):
 try:
  raw=subprocess.check_output(['git','show',args.ref+':'+name],cwd=ROOT,stderr=subprocess.DEVNULL) if args.ref else (ROOT/name).read_bytes()
  if len(raw)>8_000_000 or b'\x00' in raw:continue
  text=raw.decode('utf8')
 except (OSError,UnicodeDecodeError,subprocess.CalledProcessError):continue
 for number,line in enumerate(text.splitlines(),1):
  rules=[]
  if any(private_host(m.group()) for m in ip_pattern.finditer(line)):rules.append('private-address')
  if key_pattern.search(line):rules.append('private-key')
  if token_pattern.search(line):rules.append('credential-token')
  if re.search(r'\$\{auth\.jwt\.secret:[^}]+\}',line):rules.append('jwt-secret-fallback')
  if pathlib.Path(name).suffix in ('.yml','.yaml','.properties','.env') or pathlib.Path(name).name.startswith('.env'):
   match=assignment.match(line)
   if match:
    value=match[2].strip().strip('"\x27')
    if value and not value.startswith(('$','<','[')) and value not in ('null','false','true','NOT-SET-YET'):rules.append('literal-credential-setting')
  if re.search(r'(?i)<(?:password|passphrase)>[^<$][^<]+</',line):rules.append('xml-credential')
  if re.search(r'(?i)https?://[^/\s:@]+:[^/\s@]+@',line):rules.append('url-credential')
  if rules:findings.append((name,number,','.join(rules)))
for name,number,rules in findings:print(f'{name}:{number}: {rules}')
print(f'Checked {len(list(filter(None,names)))} files; {len(findings)} findings.',file=sys.stderr)
raise SystemExit(bool(findings))

