"""Authenticated Web API and durable operator queue for Oracle Work Order imports."""
import argparse
import csv
import io
import json
import os
import threading
import time
import urllib.parse
import uuid
from pathlib import Path
from http.server import BaseHTTPRequestHandler

import adapter
from settings_api import SettingsStore, SettingsError, Server, read_json_response

ROLE = 'Work Order Import Operator'
EXCEPTION_STATES = frozenset(('BLOCKED', 'BUSINESS_ERROR', 'UNCERTAIN', 'REJECTED'))


def may_retry(user, company_id, role=ROLE):
    return user.get('admin') is True or user.get('systemAdmin') is True or any(
        isinstance(r, dict) and r.get('enabled') is True and r.get('companyId') == company_id and
        r.get('name') in ('Admin', role) for r in user.get('roles', []))


class WorkOrderStore:
    subject = 'Work Order'
    section = 'oracleWorkOrders'
    kind = 'work-orders'
    number_key = 'workOrderNumber'
    raw_number_key = 'WORK_ORDER_NUMBER'
    role = ROLE
    route = '/work-order-import'
    target_key = 'target_work_order'
    flag = 'workOrderExceptionHandling'
    guard_class = adapter.ExistingWorkOrderGuard

    def __init__(self, config):
        self.config = dict(config, **{self.flag: True})
        self.directory = Path(config['stateDirectory'])
        self.jobs = self.directory / 'operator-jobs'
        self.jobs.mkdir(mode=0o700, parents=True, exist_ok=True)
        self.audit_reports = self.directory / 'operator-reports'
        self.audit_reports.mkdir(mode=0o700, parents=True, exist_ok=True)
        self.company_id = config[self.section]['companyId']
        self.warehouse_id = config[self.section]['warehouseId']
        self.mutex = threading.Lock()
        auth_config = {'settingsApi': config['operatorApi'], 'oracleItems': {
            'itemTypeMappingFile': str(self.directory / 'unused-mapping.json'),
            'inventoryBaseUrl': config[self.section]['inventoryBaseUrl'],
            'companyId': self.company_id, 'warehouseId': self.warehouse_id}}
        self.auth = SettingsStore(auth_config)

    def raw_rows(self, content):
        return list(csv.DictReader(io.StringIO(content.decode('utf-8-sig'))))

    def authorize(self, headers, writing=False):
        if headers.get('warehouseId') != str(self.warehouse_id):
            raise SettingsError(403, 'Select the configured import warehouse')
        token = headers.get('Authorization', '')
        token = token[7:] if token.startswith('Bearer ') else ''
        user = self.auth.authorize(headers.get('X-CWMS-Username', ''), token,
                                   headers.get('companyId', ''), require_admin=False,
                                   required_role=self.role if writing else None, return_user=True)
        if not may_retry(user, self.company_id, self.role):
            scope = self.config[self.section]['mapping']
            url = (self.config['operatorApi']['layoutBaseUrl'].rstrip('/') + '/warehouses/accessible/' +
                   urllib.parse.quote(scope['companyCode'], safe='') + '/' +
                   urllib.parse.quote(user['username'], safe=''))
            response = read_json_response(url, token)
            rows = response.get('data') if response.get('result') == 0 else None
            if not isinstance(rows, list) or not any(str(r.get('id')) == str(self.warehouse_id)
                                                     for r in rows if isinstance(r, dict)):
                raise SettingsError(403, 'Warehouse access required')
        return user

    def filename(self, name):
        if not isinstance(name, str) or not adapter.published_file(name, self.config):
            raise SettingsError(400, 'Invalid ' + self.subject + ' batch filename')
        if not (self.directory / name).is_file() and not (self.directory / (name + '.report.json')).is_file():
            raise SettingsError(404, 'Batch not found')
        return name

    def report(self, name):
        self.filename(name)
        path = self.directory / (name + '.report.json')
        report = json.loads(path.read_text()) if path.exists() else {'file': name, 'records': []}
        audit = self.audit_reports / (name + '.json')
        if audit.exists():
            prior = {r['recordId']: r for r in json.loads(audit.read_text()).get('records', [])}
            for record in report.get('records', []):
                if record.get('state') in ('BLOCKED', 'BUSINESS_ERROR', 'UNCERTAIN'):
                    for key in ('validationError', 'missingItems', 'mesError'):
                        if key not in record:
                            record[key] = prior.get(record.get('recordId'), {}).get(key, [] if key == 'missingItems' else '')
        return report

    def listing(self, query):
        batch, number, state = (query.get(k, [''])[0] for k in ('batch', 'number', 'state'))
        rows = []
        for path in sorted(self.directory.glob('*.report.json'), reverse=True):
            name = path.name[:-len('.report.json')]
            if not adapter.published_file(name, self.config) or batch.casefold() not in name.casefold():
                continue
            report = self.report(name)
            records = report.get('records', []) or [{self.number_key: '',
                                                     'state': report.get('state', 'NOT_CHECKED'),
                                                     'validationError': report.get('reason', '')}]
            for record in records:
                if record.get('state') not in EXCEPTION_STATES:
                    continue
                if number.casefold() not in record.get(self.number_key, '').casefold():
                    continue
                if state and state != record.get('state'):
                    continue
                rows.append(dict(record, file=name, sourceCleanup=report.get('sourceCleanup', ''),
                                 checkedAt=path.stat().st_mtime))
        return {'rows': rows[:1000], 'total': len(rows), 'truncated': len(rows) > 1000}

    def detail(self, name, number=''):
        report = self.report(name)
        snapshot = self.directory / name
        records, raw = [], []
        if snapshot.exists():
            content = snapshot.read_bytes()
            raw = self.raw_rows(content)
            try:
                _, records = adapter.parse_published_file(name, content, adapter.effective_config(self.config))
            except adapter.InvalidFile:
                pass
        if number:
            records = [r for r in records if r[self.number_key] == number]
            raw = [r for r in raw if r.get(self.raw_number_key, '').strip() == number]
            report = dict(report, records=[r for r in report.get('records', [])
                                          if r.get(self.number_key) == number])
        history = [j for j in self.job_list() if j['file'] == name and
                   (not number or not j.get(self.number_key) or j[self.number_key] == number)]
        return {'report': report, 'converted': records, 'originalRows': raw, 'history': history}

    def job_list(self):
        return sorted((json.loads(p.read_text()) for p in self.jobs.glob('*.json')),
                      key=lambda job: job['createdAt'], reverse=True)

    def write_job(self, job):
        adapter.atomic_write(self.jobs / (job['id'] + '.json'), adapter.canonical(job).encode())

    def enqueue(self, payload, user):
        if not isinstance(payload, dict) or set(payload) - {'file', self.number_key, 'action'}:
            raise SettingsError(400, 'Invalid action request')
        name = self.filename(payload.get('file'))
        action = payload.get('action')
        number = payload.get(self.number_key, '')
        if action not in ('check', 'retry') or not isinstance(number, str) or len(number) > 128:
            raise SettingsError(400, 'Choose check or retry and a valid ' + self.subject)
        if number:
            snapshot = self.directory / name
            if not snapshot.exists():
                raise SettingsError(409, 'Check this batch before choosing an ' + self.subject)
            _, records = adapter.parse_published_file(name, snapshot.read_bytes(), adapter.effective_config(self.config))
            if not any(r[self.number_key] == number for r in records):
                raise SettingsError(404, self.subject + ' not found in this batch')
        with self.mutex:
            for old in self.job_list():
                if old['state'] in ('QUEUED', 'RUNNING') and old['file'] == name and (
                        not number or not old.get(self.number_key) or old[self.number_key] == number):
                    raise SettingsError(409, 'An action for this selection is already queued or running')
            job = {'id': uuid.uuid4().hex, 'file': name, self.number_key: number,
                   'action': action, 'actor': user['username'], 'state': 'QUEUED',
                   'createdAt': time.time(), 'updatedAt': time.time()}
            self.write_job(job)
        return job

    def run_next(self):
        pending = sorted((j for j in self.job_list() if j['state'] == 'QUEUED' and
                          j.get('notBefore', 0) <= time.time()), key=lambda j: j['createdAt'])
        if not pending:
            return False
        job = pending[0]
        entered = False
        try:
            with adapter.state_lock(self.directory):
                entered = True
                job.update(state='RUNNING', updatedAt=time.time(), waitingReason='')
                self.write_job(job)
                token_env = self.config['mes'].get('bearerTokenEnv')
                token = os.environ[token_env] if token_env else None
                api = adapter.MesAPI(self.config['mes']['baseUrl'], token,
                                     self.config[self.section]['mapping']['companyCode'])
                guard = self.guard_class(self.config[self.section], token)
                adapter.poll_once(self.config, self.directory, api, guard, target_file=job['file'],
                                  **{self.target_key: job.get(self.number_key) or None},
                                  check_only=job['action'] == 'check', retry_failed=job['action'] == 'retry')
                report = self.report(job['file'])
                adapter.atomic_write(self.audit_reports / (job['file'] + '.json'),
                                     adapter.canonical(report).encode())
                if report.get('state') == 'WAITING_STABLE':
                    job.update(state='QUEUED', notBefore=time.time() + self.config.get('stableSeconds', 60))
                else:
                    results = [r for r in report.get('records', []) if not job.get(self.number_key) or
                               r.get(self.number_key) == job[self.number_key]]
                    job.update(state='FINISHED', results=results, batchState=report.get('state', ''),
                               error=report.get('reason', ''))
        except Exception as error:
            if not entered:
                if not job.get('waitingReason'):
                    job.update(waitingReason='Waiting for the scheduled import worker to finish',
                               updatedAt=time.time())
                    self.write_job(job)
                return False  # Daily importer owns the same lock; leave this job queued.
            job.update(state='FAILED', error='Import action failed (' + type(error).__name__ + '). Recheck before retrying.')
        job['updatedAt'] = time.time()
        self.write_job(job)
        return True

    def refresh_processing(self):
        """Refresh accepted records using GET only; never submit or delete sources."""
        try:
            with adapter.state_lock(self.directory):
                token_env = self.config['mes'].get('bearerTokenEnv')
                api = adapter.MesAPI(self.config['mes']['baseUrl'], os.environ[token_env] if token_env else None,
                                     self.config[self.section]['mapping']['companyCode'])
                ledger = adapter.Ledger(self.directory / 'ledger.sqlite3')
                try:
                    adapter.refresh_status(ledger, api)
                    for path in self.directory.glob('*.report.json'):
                        name = path.name[:-len('.report.json')]
                        if not adapter.published_file(name, self.config):
                            continue
                        report = self.report(name)
                        before = adapter.canonical(report)
                        for row in report.get('records', []):
                            status = ledger.get(self.kind, row['recordId'])
                            if status:
                                if status['state'] == 'BUSINESS_ERROR' and status['integrationId']:
                                    try:
                                        business = api.status(self.kind, status['integrationId'])
                                        state = {'COMPLETED': 'COMPLETED', 'ERROR': 'BUSINESS_ERROR'}.get(business, 'ACCEPTED')
                                        ledger.set(self.kind, row['recordId'], state, status['integrationId'], business)
                                        status = ledger.get(self.kind, row['recordId'])
                                    except Exception:
                                        pass
                                row.update(status)
                                detail = getattr(api, 'status_details', {}).get(status['integrationId'])
                                if detail:
                                    row['mesError'] = detail['errorMessage']
                                if status['state'] in ('ACCEPTED', 'COMPLETED', 'SKIPPED_EXISTING'):
                                    row.update(validationError='', missingItems=[], mesError='')
                        if adapter.canonical(report) != before:
                            adapter.atomic_write(path, adapter.canonical(report).encode())
                            adapter.atomic_write(self.audit_reports / (name + '.json'), adapter.canonical(report).encode())
                finally:
                    ledger.db.close()
            return True
        except ValueError:
            return False

    def worker(self):
        for job in self.job_list():
            if job['state'] == 'RUNNING':
                job.update(state='INTERRUPTED', updatedAt=time.time(),
                           error='Service restarted; recheck the selection before retrying')
                self.write_job(job)
        next_refresh = 0
        while True:
            try:
                self.run_next()
                if time.monotonic() >= next_refresh:
                    self.refresh_processing()
                    next_refresh = time.monotonic() + 30
            except Exception:
                print('Operator queue temporarily unavailable', flush=True)
            time.sleep(2)


class Handler(BaseHTTPRequestHandler):
    def reply(self, status, data=None, message=''):
        body = adapter.canonical({'result': 0 if status < 400 else 1, 'message': message, 'data': data}).encode()
        self.send_response(status)
        for key, value in {'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store',
                           'X-Content-Type-Options': 'nosniff', 'Content-Length': str(len(body))}.items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(body)

    def handle_request(self, writing=False):
        try:
            parsed = urllib.parse.urlsplit(self.path)
            if not writing and parsed.path == '/healthz':
                self.reply(200, {'healthy': True})
                return
            user = self.server.store.authorize(self.headers, writing)
            query = urllib.parse.parse_qs(parsed.query)
            if writing and parsed.path == self.server.store.route + '/actions':
                length = int(self.headers.get('Content-Length', '0'))
                if not 0 < length <= 4096:
                    raise SettingsError(400, 'Invalid request size')
                job = self.server.store.enqueue(json.loads(self.rfile.read(length)), user)
                self.reply(202, job)
            elif not writing and parsed.path == self.server.store.route + '/batches':
                result = self.server.store.listing(query)
                result['canRetry'] = may_retry(user, self.server.store.company_id, self.server.store.role)
                self.reply(200, result)
            elif not writing and parsed.path == self.server.store.route + '/batch':
                self.reply(200, self.server.store.detail(query.get('file', [''])[0], query.get('number', [''])[0]))
            elif not writing and parsed.path == self.server.store.route + '/jobs':
                self.reply(200, self.server.store.job_list()[:100])
            else:
                raise SettingsError(404, 'Not found')
        except SettingsError as error:
            self.reply(error.status, message=str(error))
        except (ValueError, json.JSONDecodeError):
            self.reply(400, message='Invalid request')
        except Exception:
            self.reply(502, message='Could not complete the import request')

    def do_GET(self):
        self.handle_request()

    def do_POST(self):
        self.handle_request(True)

    def log_message(self, *args):
        pass


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    args = parser.parse_args()
    config = json.loads(args.config.read_text())
    store = WorkOrderStore(config)
    server = Server((config['operatorApi']['bind'], config['operatorApi']['port']), Handler)
    server.store = store
    threading.Thread(target=store.worker, daemon=True).start()
    server.serve_forever()


if __name__ == '__main__':
    main()
