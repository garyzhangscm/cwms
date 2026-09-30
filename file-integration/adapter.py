"""File-to-CWMS adapter. Python 3.10+, standard library only; no Oracle driver."""
import argparse
import csv
import ftplib
import hashlib
import io
import json
import os
import re
import sqlite3
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from contextlib import contextmanager
from pathlib import Path


COMMON = {'companyCode', 'warehouseName', 'clientName'}
SCHEMAS = {
    'suppliers': (COMMON | {'name', 'description', 'contactorFirstname',
        'contactorLastname', 'addressCountry', 'addressState', 'addressCity',
        'addressLine1', 'addressLine2', 'addressPostcode'}, set(), None, 'name'),
    'items': (COMMON | {'name', 'description', 'unitCost', 'nonInventoryItem'},
              set(), None, 'name'),
    'item-package-types': (COMMON | {'name', 'description', 'itemName', 'supplierName'},
        {'unitOfMeasureName', 'quantity', 'length', 'width', 'height', 'weight'},
        'itemUnitOfMeasures', 'name'),
    'receipts': (COMMON | {'number', 'supplierName', 'allowUnexpectedItem'},
        {'number', 'itemName', 'expectedQuantity', 'overReceivingQuantity',
         'overReceivingPercent'}, 'receiptLines', 'number'),
    'orders': (COMMON | {'number', 'poNumber', 'shipToCustomerName',
        'billToCustomerName', 'shipToAddressCountry', 'shipToAddressState',
        'shipToAddressCity', 'shipToAddressLine1', 'shipToAddressPostcode'},
        {'number', 'itemName', 'expectedQuantity', 'inventoryStatusName'},
        'orderLines', 'number'),
    'work-orders': (COMMON | {'number', 'itemName', 'poNumber', 'expectedQuantity'},
        {'number', 'itemName', 'expectedQuantity', 'inventoryStatusName'},
        'workOrderLines', 'number'),
}
INT_FIELDS = {'expectedQuantity', 'overReceivingQuantity', 'quantity'}
NUMBER_FIELDS = {'unitCost', 'overReceivingPercent', 'length', 'width', 'height', 'weight'}
BOOL_FIELDS = {'allowUnexpectedItem', 'nonInventoryItem'}
MAX_BYTES = 10 * 1024 * 1024
MAX_ROWS = 10000
FILE_RE = re.compile(r'^(suppliers|items|item-package-types|receipts|orders|work-orders)__(\w[\w.-]{0,100})\.(csv|xml)$', re.ASCII)


class InvalidFile(ValueError):
    pass


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def typed(field, value):
    value = value.strip()
    if field in INT_FIELDS:
        if not re.fullmatch(r'\d+', value):
            raise InvalidFile('integer required: ' + field)
        result = int(value)
        if result > (2147483647 if field == 'quantity' else 9223372036854775807):
            raise InvalidFile('integer out of range: ' + field)
        return result
    if field in NUMBER_FIELDS:
        import math
        try:
            result = float(value)
        except ValueError:
            raise InvalidFile('number required: ' + field) from None
        if not math.isfinite(result) or result < 0:
            raise InvalidFile('non-negative finite number required: ' + field)
        return result
    if field in BOOL_FIELDS:
        if value.lower() not in ('true', 'false'):
            raise InvalidFile('true/false required: ' + field)
        return value.lower() == 'true'
    return value


def xml_rows(text):
    # UTF-8 only; rejecting declarations also prevents external entities/DTD expansion.
    if re.search(r'<!\s*(DOCTYPE|ENTITY)', text, re.I):
        raise InvalidFile('DTD/entities are not allowed')
    try:
        root = ET.fromstring(text)
    except ET.ParseError:
        raise InvalidFile('invalid XML') from None
    if root.tag != 'batch' or root.attrib or (root.text or '').strip():
        raise InvalidFile('XML root must be <batch> without attributes')
    result = []
    for record in root:
        if (record.tail or '').strip():
            raise InvalidFile('unexpected XML text')
        if record.tag != 'record' or record.attrib or (record.text or '').strip():
            raise InvalidFile('expected <record>')
        row = {}
        for field in record:
            if (field.tail or '').strip():
                raise InvalidFile('unexpected XML text')
            fields = list(field) if field.tag == 'line' else [field]
            if field.tag == 'line' and (field.attrib or (field.text or '').strip()):
                raise InvalidFile('invalid line element')
            for leaf in fields:
                key = ('line.' if field.tag == 'line' else '') + leaf.tag
                if key in row or leaf.attrib or len(leaf) or (leaf.tail or '').strip():
                    raise InvalidFile('duplicate or nested XML field')
                row[key] = leaf.text or ''
        result.append(row)
    return result


def parse_file(name, content):
    """Validate the entire file before constructing any API requests."""
    match = FILE_RE.fullmatch(name)
    if not match or len(content) > MAX_BYTES:
        raise InvalidFile('invalid filename or file too large')
    kind, batch, fmt = match.groups()
    try:
        text = content.decode('utf-8-sig')
    except UnicodeDecodeError:
        raise InvalidFile('file must be UTF-8') from None
    if fmt == 'csv':
        csv.field_size_limit(MAX_BYTES)
        reader = csv.DictReader(io.StringIO(text, newline=''), strict=True)
        try:
            columns = reader.fieldnames
            if not columns or len(set(columns)) != len(columns):
                raise InvalidFile('missing or duplicate CSV columns')
            rows = list(reader)
        except csv.Error:
            raise InvalidFile('invalid CSV') from None
    else:
        rows = xml_rows(text)
    if not rows or len(rows) > MAX_ROWS:
        raise InvalidFile('file must have 1..10000 rows')
    headers, line_fields, array_key, name_key = SCHEMAS[kind]
    allowed = headers | {'recordId'} | {'line.' + k for k in line_fields}
    grouped = {}
    line_ids = {}
    for row in rows:
        if set(row) - allowed or any(v is None or not isinstance(v, str) for v in row.values()):
            raise InvalidFile('unknown column or inconsistent row width')
        row = {k: v.strip() for k, v in row.items() if v.strip()}
        required = {'recordId', 'companyCode', 'warehouseName', name_key}
        if kind in ('work-orders', 'item-package-types'):
            required.add('itemName')
        if kind == 'work-orders':
            required.add('expectedQuantity')
        if not required <= row.keys():
            raise InvalidFile('required header fields missing: ' + ', '.join(sorted(required)))
        rid = row['recordId']
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}', rid):
            raise InvalidFile('invalid recordId')
        header = {k: typed(k, v) for k, v in row.items() if k in headers}
        if rid in grouped and grouped[rid]['header'] != header:
            raise InvalidFile('inconsistent header for repeated recordId')
        if rid in grouped and not array_key:
            raise InvalidFile('duplicate recordId in master-data file')
        grouped.setdefault(rid, {'header': header, 'lines': []})
        if array_key:
            line = {k[5:]: typed(k[5:], v) for k, v in row.items() if k.startswith('line.')}
            required_line = {'unitOfMeasureName', 'quantity'} if kind == 'item-package-types' else {'number', 'itemName', 'expectedQuantity'}
            if not required_line <= line.keys():
                raise InvalidFile('required line fields missing')
            line_id = line.get('number', line.get('unitOfMeasureName'))
            if line_id in line_ids.setdefault(rid, set()):
                raise InvalidFile('duplicate line number/unit for recordId')
            line_ids[rid].add(line_id)
            grouped[rid]['lines'].append(line)
    records = []
    for rid, group in grouped.items():
        payload = dict(group['header'])
        if array_key:
            # Stable order makes CSV/XML and line reordering produce the same hash.
            payload[array_key] = sorted(group['lines'], key=lambda x: str(x.get('number', x.get('unitOfMeasureName'))))
            for line in payload[array_key]:
                line.update({k: payload[k] for k in ('companyCode', 'warehouseName')})
        # This endpoint accepts the integration entity itself, without a DTO
        # constructor to initialize workflow status.
        if kind == 'work-orders':
            payload['status'] = 'PENDING'
            for line in payload[array_key]:
                line['status'] = 'ATTACHED'
        records.append({'kind': kind, 'recordId': rid, 'payload': payload})
    return batch, records


class Ledger:
    def __init__(self, path):
        self.db = sqlite3.connect(path)
        self.db.execute('PRAGMA synchronous=FULL')
        self.db.execute('''CREATE TABLE IF NOT EXISTS records (
            kind TEXT, record_id TEXT, digest TEXT NOT NULL, state TEXT NOT NULL,
            integration_id TEXT, business_status TEXT, PRIMARY KEY(kind,record_id))''')
        self.db.execute("UPDATE records SET state='UNCERTAIN' WHERE state='SENDING'")
        self.db.commit()

    def prepare(self, records):
        with self.db:
            for record in records:
                digest = hashlib.sha256(canonical(record['payload']).encode()).hexdigest()
                key = (record['kind'], record['recordId'])
                old = self.db.execute('SELECT digest FROM records WHERE kind=? AND record_id=?', key).fetchone()
                if old and old[0] != digest:
                    raise InvalidFile('recordId reused with different content; no records from this file submitted')
                self.db.execute('INSERT OR IGNORE INTO records(kind,record_id,digest,state) VALUES (?,?,?,?)', (*key, digest, 'PREPARED'))

    def get(self, kind, rid):
        row = self.db.execute('SELECT state,integration_id,business_status FROM records WHERE kind=? AND record_id=?', (kind, rid)).fetchone()
        return dict(zip(('state', 'integrationId', 'businessStatus'), row)) if row else None

    def set(self, kind, rid, state, integration_id=None, business_status=None):
        with self.db:
            self.db.execute('UPDATE records SET state=?,integration_id=?,business_status=? WHERE kind=? AND record_id=?',
                            (state, integration_id, business_status, kind, rid))


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class MesAPI:
    def __init__(self, url, token=None):
        parsed = urllib.parse.urlsplit(url)
        if parsed.scheme not in ('http', 'https') or not parsed.netloc or parsed.username or parsed.query or parsed.fragment:
            raise ValueError('invalid MES base URL')
        self.url = url.rstrip('/')
        self.token = token
        self.opener = urllib.request.build_opener(NoRedirect())

    def request(self, method, path, payload=None):
        headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
        if self.token:
            headers['Authorization'] = 'Bearer ' + self.token
        request = urllib.request.Request(self.url + path, data=canonical(payload).encode() if payload is not None else None,
                                         headers=headers, method=method)
        with self.opener.open(request, timeout=30) as response:
            raw = response.read(MAX_BYTES + 1)
            if len(raw) > MAX_BYTES:
                raise ValueError('response too large')
            obj = json.loads(raw)
        # CWMS wraps both success and some failures in HTTP 200.
        if not isinstance(obj, dict) or type(obj.get('result')) is not int or obj['result'] != 0 or 'data' not in obj:
            raise ValueError('MES returned a business error or unexpected response')
        return obj['data']

    def submit(self, kind, payload):
        data = self.request('PUT', '/integration-data/' + kind, payload)
        identity = data.get('id') if isinstance(data, dict) else data
        if isinstance(identity, bool) or not re.fullmatch(r'[1-9][0-9]*', str(identity)):
            raise ValueError('MES did not return a valid integration record ID')
        status = data.get('status') if isinstance(data, dict) else None
        return str(identity), status

    def status(self, kind, identity):
        data = self.request('GET', '/integration-data/' + kind + '/' + identity)
        if not isinstance(data, dict) or str(data.get('id')) != identity or not isinstance(data.get('status'), str):
            raise ValueError('unexpected status response')
        return data['status']


def submit_records(records, ledger, api):
    ledger.prepare(records)
    for record in records:
        kind, rid = record['kind'], record['recordId']
        if ledger.get(kind, rid)['state'] != 'PREPARED':
            continue
        ledger.set(kind, rid, 'SENDING')
        try:
            identity, status = api.submit(kind, record['payload'])
        except Exception:
            # A failed response does not prove the server rolled back. Never blind-retry.
            ledger.set(kind, rid, 'UNCERTAIN')
            break
        ledger.set(kind, rid, 'ACCEPTED', identity, status)
    return [{'kind': r['kind'], 'recordId': r['recordId'], **ledger.get(r['kind'], r['recordId'])} for r in records]


def refresh_status(ledger, api):
    records = ledger.db.execute("SELECT kind,record_id,integration_id FROM records WHERE state='ACCEPTED'").fetchall()
    for kind, rid, identity in records:
        try:
            status = api.status(kind, identity)
        except Exception:
            continue
        state = {'COMPLETED': 'COMPLETED', 'ERROR': 'BUSINESS_ERROR'}.get(status, 'ACCEPTED')
        ledger.set(kind, rid, state, identity, status)


@contextmanager
def state_lock(directory):
    import fcntl
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    with open(directory / 'worker.lock', 'a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('another worker is using this state directory') from None
        yield


def atomic_write(path, data):
    temp = path.with_name(path.name + '.tmp')
    with open(temp, 'wb') as f:
        f.write(data)
        f.flush()
        os.fsync(f.fileno())
    os.replace(temp, path)


def ftp_connect(config):
    cls = ftplib.FTP_TLS if config.get('tls', False) else ftplib.FTP
    ftp = cls(context=ssl.create_default_context()) if cls is ftplib.FTP_TLS else cls()
    try:
        ftp.connect(config['host'], config.get('port', 21), timeout=30)
        ftp.login(os.environ[config['usernameEnv']], os.environ[config['passwordEnv']])
        if cls is ftplib.FTP_TLS:
            ftp.prot_p()
        ftp.cwd(config['inbox'])
        return ftp
    except BaseException:
        ftp.close()
        raise


def download(ftp, name):
    buffer = io.BytesIO()
    def append(chunk):
        if buffer.tell() + len(chunk) > MAX_BYTES:
            raise InvalidFile('remote file exceeds size limit')
        buffer.write(chunk)
    ftp.retrbinary('RETR ' + name, append)
    return buffer.getvalue()


def poll_once(config, directory, api):
    """One bounded scan. Source files are left intact; .ready publishes completion."""
    ledger = Ledger(directory / 'ledger.sqlite3')
    ftp = None
    rejected = set()
    try:
        ftp = ftp_connect(config['ftp'])
        names = set(ftp.nlst())
        for name in sorted(names):
            if not FILE_RE.fullmatch(name) or name + '.ready' not in names:
                continue
            snapshot = directory / name
            try:
                content = download(ftp, name)
                if snapshot.exists() and snapshot.read_bytes() != content:
                    raise InvalidFile('published filename changed; use a new batch filename')
                _, records = parse_file(name, content)
                # Preflight before accepting immutable local snapshot.
                ledger.prepare(records)
                if not snapshot.exists():
                    atomic_write(snapshot, content)
                submit_records(records, ledger, api)
                print(json.dumps({'file': name, 'state': 'INSPECTED'}), flush=True)
            except InvalidFile as error:
                rejected.add(name)
                print(json.dumps({'file': name, 'state': 'REJECTED', 'reason': str(error)}), flush=True)
                atomic_write(directory / (name + '.report.json'), canonical({'file': name, 'state': 'REJECTED', 'reason': str(error)}).encode())
        refresh_status(ledger, api)
        # Rebuild reports even when the producer has removed previously fetched files.
        for snapshot in directory.iterdir():
            if not FILE_RE.fullmatch(snapshot.name) or snapshot.name in rejected:
                continue
            _, records = parse_file(snapshot.name, snapshot.read_bytes())
            report = {'file': snapshot.name, 'records': [
                {'recordId': r['recordId'], **ledger.get(r['kind'], r['recordId'])} for r in records]}
            atomic_write(directory / (snapshot.name + '.report.json'), canonical(report).encode())
    finally:
        if ftp:
            ftp.close()
        ledger.db.close()


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    check = sub.add_parser('validate', help='offline; no FTP or MES calls')
    check.add_argument('file', type=Path)
    run = sub.add_parser('run', help='download and submit using configured MES API')
    run.add_argument('--config', type=Path, required=True)
    run.add_argument('--send', action='store_true', help='explicitly enable MES writes')
    run.add_argument('--once', action='store_true')
    args = parser.parse_args()
    if args.command == 'validate':
        with args.file.open('rb') as f:
            batch, records = parse_file(args.file.name, f.read(MAX_BYTES + 1))
        print(json.dumps({'batch': batch, 'kind': records[0]['kind'], 'records': len(records), 'valid': True}))
        return
    if not args.send:
        parser.error('run requires --send; use validate for offline checks')
    config = json.loads(args.config.read_text())
    if config.get('protocolVersion') != 1:
        raise ValueError('protocolVersion must be 1')
    if not config['ftp'].get('tls') and not config['ftp'].get('allowPlainFtp'):
        raise ValueError('plain FTP requires allowPlainFtp=true')
    token_env = config['mes'].get('bearerTokenEnv')
    api = MesAPI(config['mes']['baseUrl'], os.environ[token_env] if token_env else None)
    directory = Path(config['stateDirectory']).resolve()
    interval = config.get('pollSeconds', 60)
    if not isinstance(interval, int) or interval < 10:
        raise ValueError('pollSeconds must be at least 10')
    with state_lock(directory):
        while True:
            try:
                poll_once(config, directory, api)
            except Exception as error:
                # Never emit remote response bodies, credentials, or business payloads.
                print(json.dumps({'state': 'SCAN_FAILED', 'errorType': type(error).__name__}), flush=True)
                if args.once:
                    return 1
            if args.once:
                return 0
            time.sleep(interval)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        sys.exit(130)
    except Exception as error:
        print('Failed: ' + type(error).__name__, file=sys.stderr)
        sys.exit(1)
