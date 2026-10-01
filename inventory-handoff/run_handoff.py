"""Move configured external-handoff inventory to MES INVADJ with an audit trail."""
import argparse
import datetime
import fcntl
import json
import os
import tempfile
import time
import urllib.parse
import urllib.request
from pathlib import Path

from settings_api import Store, canonical


ACTOR = 'CWMS-EXTERNAL-HANDOFF'
MAX_INVENTORIES_PER_RUN = 5000
MAX_RESPONSE_BYTES = 32 * 1024 * 1024


class HandoffError(Exception):
    pass


def request_json(url, method='GET', username=None, timeout=60, data=None):
    headers = {'Accept': 'application/json'}
    if username:
        headers['username'] = username
    if data is not None:
        headers['Content-Type'] = 'text/plain; charset=utf-8'
        data = data.encode('utf-8')
    request = urllib.request.Request(url, headers=headers, method=method, data=data)
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read(MAX_RESPONSE_BYTES + 1)
    if len(body) > MAX_RESPONSE_BYTES:
        raise HandoffError('MES response exceeded size limit')
    return json.loads(body)


def inventory_rows(base, warehouse_id, params):
    query = urllib.parse.urlencode({'warehouseId': warehouse_id,
                                    'includeDetails': 'false', **params})
    response = request_json(base + '/inventories?' + query)
    rows = response.get('data') if isinstance(response, dict) and response.get('result') == 0 else None
    if not isinstance(rows, list):
        raise HandoffError('MES inventories response was invalid')
    return rows


def inventory_count(base, warehouse_id, location_id):
    query = urllib.parse.urlencode({'warehouseId': warehouse_id, 'locationId': location_id})
    response = request_json(base + '/inventories/count?' + query)
    count = response.get('data') if isinstance(response, dict) and response.get('result') == 0 else None
    if type(count) is not int or count < 0:
        raise HandoffError('MES inventory count response was invalid')
    return count


def row_problem(row, location_id, warehouse_id):
    if not isinstance(row, dict) or type(row.get('id')) is not int or row['id'] <= 0:
        return 'invalid inventory ID'
    if row.get('warehouseId') != warehouse_id or row.get('locationId') != location_id:
        return 'inventory is outside configured warehouse or location'
    if not isinstance(row.get('lpn'), str) or not row['lpn']:
        return 'missing LPN'
    if type(row.get('quantity')) is not int or row['quantity'] <= 0:
        return 'quantity is not a positive integer'
    if row.get('virtual') is True:
        return 'virtual inventory'
    if row.get('lockedForAdjust') is not False or row.get('locks'):
        return 'inventory is locked'
    if row.get('allocatedByPickId') is not None or row.get('pickNumber'):
        return 'inventory is allocated for picking'
    return None


def write_report(path, report):
    fd, temp_name = tempfile.mkstemp(prefix='.handoff-report-', dir=str(path.parent))
    try:
        os.fchmod(fd, 0o600)
        with os.fdopen(fd, 'w') as stream:
            stream.write(canonical(report) + '\n')
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temp_name, str(path))
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)


def activity_confirmed(base, warehouse_id, lpn, document_number):
    query = urllib.parse.urlencode({'warehouseId': warehouse_id, 'lpn': lpn,
                                    'includeDetails': 'false'})
    response = request_json(base + '/inventory-activities?' + query)
    rows = response.get('data') if isinstance(response, dict) and response.get('result') == 0 else None
    if not isinstance(rows, list):
        raise HandoffError('MES inventory activity response was invalid')
    return any(row.get('documentNumber') == document_number and
               row.get('type') == 'INVENTORY_ADJUSTMENT' and
               row.get('username') == ACTOR for row in rows if isinstance(row, dict))


def adjustment_confirmed(base, warehouse_id, inventory_id, lpn, adjustment_location_id,
                         document_number):
    # The DELETE can finish on MES after its HTTP client has timed out. Never
    # send it again; retry only these read-only confirmation requests.
    for attempt in range(3):
        try:
            rows = inventory_rows(base, warehouse_id,
                                  {'inventoryIds': inventory_id, 'includeVirturalInventory': 'true'})
            if (len(rows) == 1 and rows[0].get('id') == inventory_id and
                    rows[0].get('lpn') == lpn and
                    rows[0].get('locationId') == adjustment_location_id and
                    rows[0].get('virtual') is True and
                    activity_confirmed(base, warehouse_id, lpn, document_number)):
                return True
        except Exception:
            pass
        if attempt < 2:
            time.sleep(5)
    return False


def submit_batch(config, candidates, report, report_path, execute):
    # Never resubmit a previous mutation, including an interrupted HTTP request.
    attempted = set()
    for path in Path(config['reportDirectory']).glob('*-execute.json'):
        if path == report_path:
            continue
        previous = json.loads(path.read_text())
        for row in previous.get('records', []):
            if row.get('status') in ('SUBMITTED', 'COMPLETED', 'UNCERTAIN',
                                      'SENT_UNVERIFIED', 'MOVED_UNVERIFIED', 'BATCH_ACCEPTED'):
                attempted.add(row['id'])
    eligible = []
    for location, row in candidates:
        record = {'id': row['id'], 'lpn': row.get('lpn'), 'locationId': location['id']}
        report['records'].append(record)
        problem = row_problem(row, location['id'], config['warehouseId'])
        if row['id'] in attempted:
            problem = 'previously submitted; not resubmitted'
        if problem:
            record.update(status='SKIPPED', reason=problem)
        else:
            record['status'] = 'READY'
            eligible.append(record)
    if not execute or not eligible:
        report['status'] = 'COMPLETED'
        return report_path, report
    for record in eligible:
        record['status'] = 'SENT_UNVERIFIED'
    write_report(report_path, report)
    query = urllib.parse.urlencode({'companyId': config['companyId'], 'asyncronized': 'true'})
    try:
        response = request_json(config['inventoryBaseUrl'].rstrip('/') + '/inventory/batch-remove?' + query,
                                method='DELETE', username=ACTOR, timeout=120,
                                data=','.join(str(record['id']) for record in eligible))
        if not isinstance(response, dict) or response.get('result') != 0 or response.get('data') != 'remove request has been sent':
            raise HandoffError('Unexpected batch response: {}'.format(str(response)[:200]))
    except Exception as error:
        for record in eligible:
            record.update(status='UNCERTAIN', reason=str(error)[:200])
        raise
    for record in eligible:
        record['status'] = 'BATCH_ACCEPTED'
    report['status'] = 'SUBMITTED'
    report['message'] = 'MES accepted the batch; background completion is not verified.'
    return report_path, report


def process(config, execute, limit=None, verify_after=False):
    if limit is not None and (type(limit) is not int or limit < 1 or limit > MAX_INVENTORIES_PER_RUN):
        raise ValueError('Limit must be between 1 and {}'.format(MAX_INVENTORIES_PER_RUN))
    store = Store(config)
    base = config['inventoryBaseUrl'].rstrip('/')
    adjustment_location_id = config['adjustmentLocationId']
    report_dir = Path(config['reportDirectory'])
    report_dir.mkdir(mode=0o750, parents=True, exist_ok=True)
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    report = {'startedAt': started, 'mode': 'execute' if execute else 'dry-run',
              'warehouseId': store.warehouse_id, 'locations': [], 'records': [],
              'status': 'RUNNING'}
    report_path = report_dir / (datetime.datetime.now().strftime('%Y%m%d-%H%M%S') +
                                ('-execute' if execute else '-dry-run') + '.json')
    lock_path = report_dir / '.run.lock'
    with open(str(lock_path), 'a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        try:
            location_ids, _ = store.read()
            if not location_ids:
                report['status'] = 'NO_LOCATIONS'
                return report_path, report
            # Revalidate selected locations at the beginning of every run.
            locations = store.resolve(location_ids)
            report['locations'] = locations
            candidates = []
            seen = set()
            for location in locations:
                rows = inventory_rows(base, store.warehouse_id,
                                      {'locationId': location['id']})
                count = inventory_count(base, store.warehouse_id, location['id'])
                if len(rows) != count:
                    raise HandoffError('Inventory count changed during location {} scan'.format(location['id']))
                for row in rows:
                    if (not isinstance(row, dict) or type(row.get('id')) is not int or
                            row['id'] <= 0 or row['id'] in seen):
                        raise HandoffError('Duplicate or invalid inventory in scan')
                    seen.add(row['id'])
                    candidates.append((location, row))
            if len(candidates) > MAX_INVENTORIES_PER_RUN:
                raise HandoffError('Inventory count exceeds per-run safety limit')
            report['candidateCount'] = len(candidates)
            candidates.sort(key=lambda entry: entry[1]['id'])
            if limit is not None:
                candidates = candidates[:limit]
            report['selectedCount'] = len(candidates)
            write_report(report_path, report)
            if config.get('submissionMode') == 'batch':
                return submit_batch(config, candidates, report, report_path, execute)
            consecutive_failures = 0
            for location, scanned in candidates:
                record = {'id': scanned['id'], 'lpn': scanned.get('lpn'),
                          'locationId': location['id']}
                report['records'].append(record)
                problem = row_problem(scanned, location['id'], store.warehouse_id)
                if problem:
                    record.update(status='SKIPPED', reason=problem)
                    write_report(report_path, report)
                    continue
                if not execute:
                    record['status'] = 'READY'
                    continue
                try:
                    current = inventory_rows(base, store.warehouse_id,
                                             {'inventoryIds': scanned['id']})
                    if len(current) != 1:
                        record.update(status='SKIPPED', reason='inventory changed or disappeared')
                        write_report(report_path, report)
                        continue
                    problem = row_problem(current[0], location['id'], store.warehouse_id)
                    if problem or current[0]['lpn'] != scanned['lpn'] or current[0]['quantity'] != scanned['quantity']:
                        record.update(status='SKIPPED', reason=problem or 'inventory changed since scan')
                        write_report(report_path, report)
                        continue
                    document = 'EXT-HANDOFF-{}'.format(scanned['id'])
                    comment = 'Transferred to external fulfillment system from location {}.'.format(location['name'])
                    query = urllib.parse.urlencode({'documentNumber': document, 'comment': comment})
                    # Mark the attempt before the mutation so an interrupted call is visible.
                    record.update(status='SENT_UNVERIFIED', documentNumber=document)
                    write_report(report_path, report)
                    mutation_error = None
                    try:
                        response = request_json(base + '/inventory-adj/{}?{}'.format(scanned['id'], query),
                                                method='DELETE', username=ACTOR, timeout=120)
                        if isinstance(response, dict) and response.get('result') not in (None, 0):
                            raise HandoffError('MES rejected inventory adjustment')
                    except Exception as error:
                        mutation_error = error
                    if verify_after:
                        record['status'] = 'MOVED_UNVERIFIED'
                        write_report(report_path, report)
                        if not adjustment_confirmed(base, store.warehouse_id, scanned['id'],
                                                    scanned['lpn'], adjustment_location_id, document):
                            raise HandoffError('MES adjustment was not confirmed: {}'.format(
                                mutation_error if mutation_error else 'inventory or activity mismatch'))
                        record['status'] = 'COMPLETED'
                    else:
                        if mutation_error:
                            raise mutation_error
                        record['status'] = 'SUBMITTED'
                    consecutive_failures = 0
                except Exception as error:
                    record.update(status='UNCERTAIN', reason=str(error)[:200])
                    consecutive_failures += 1
                write_report(report_path, report)
                if consecutive_failures >= 5:
                    raise HandoffError('Five consecutive inventory adjustments were uncertain; stopped')
            statuses = [record['status'] for record in report['records']]
            report['status'] = ('FAILED' if 'UNCERTAIN' in statuses else 'COMPLETED')
            return report_path, report
        except Exception as error:
            report.update(status='FAILED', error=str(error)[:300])
            return report_path, report
        finally:
            report['finishedAt'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
            write_report(report_path, report)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--dry-run', action='store_true')
    mode.add_argument('--execute', action='store_true')
    parser.add_argument('--limit', type=int, help='Process at most this many scanned inventories')
    parser.add_argument('--verify-after', action='store_true',
                        help='Recheck the virtual location and activity after every adjustment')
    args = parser.parse_args()
    config = json.loads(args.config.read_text())
    path, report = process(config, args.execute, args.limit, args.verify_after)
    counts = {}
    for record in report['records']:
        counts[record['status']] = counts.get(record['status'], 0) + 1
    print(canonical({'status': report['status'], 'candidateCount': report.get('candidateCount', 0),
                     'selectedCount': report.get('selectedCount', 0),
                     'counts': counts, 'report': str(path), 'error': report.get('error')}), flush=True)
    if report['status'] == 'FAILED':
        raise SystemExit(1)


if __name__ == '__main__':
    main()
