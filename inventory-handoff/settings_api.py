"""Admin-only inventory handoff location settings, intended to run on k8s-app1."""
import argparse
import base64
import binascii
import fcntl
import hashlib
import json
import os
import re
import tempfile
import time
import urllib.parse
import urllib.request
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from socketserver import ThreadingMixIn


MAX_BODY = 16384
MAX_LOCATIONS = 20
QUERY_RE = re.compile(r'[A-Za-z0-9_. -]{2,40}')


class ApiError(Exception):
    def __init__(self, status, message):
        super().__init__(message)
        self.status = status


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=False)


def fetch_json(url):
    request = urllib.request.Request(url, headers={'Accept': 'application/json'})
    with urllib.request.urlopen(request, timeout=8) as response:
        content = response.read(1024 * 1024 + 1)
    if len(content) > 1024 * 1024:
        raise ApiError(502, 'MES response was too large')
    return json.loads(content)


class Store:
    def __init__(self, config):
        self.path = Path(config['stateFile'])
        self.auth_url = config['authBaseUrl'].rstrip('/')
        self.user_url = config['userBaseUrl'].rstrip('/')
        self.layout_url = config['layoutBaseUrl'].rstrip('/')
        self.company_id = config['companyId']
        self.warehouse_id = config['warehouseId']

    def authorize(self, username, token, company_id):
        if not username or not token or len(token) > 4096 or str(self.company_id) != company_id:
            raise ApiError(401, 'MES login required')
        try:
            claims = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))
            if (claims.get('sub') != username or
                    claims.get('companyId') not in (self.company_id, -1) or
                    int(claims.get('exp', 0)) <= time.time()):
                raise ValueError('Invalid JWT claims')
        except (IndexError, ValueError, TypeError, binascii.Error):
            raise ApiError(401, 'MES login required') from None
        try:
            auth = fetch_json(self.auth_url + '/users/username-by-token?' +
                              urllib.parse.urlencode({'companyId': self.company_id, 'token': token}))
            if not isinstance(auth, dict) or auth.get('result') != 0 or auth.get('data') != username:
                raise ApiError(401, 'MES login required')
            users_response = fetch_json(self.user_url + '/users?' +
                                        urllib.parse.urlencode({'companyId': self.company_id,
                                                                'username': username}))
        except ApiError:
            raise
        except Exception:
            raise ApiError(502, 'Could not verify MES admin permission') from None
        users = users_response.get('data') if isinstance(users_response, dict) and users_response.get('result') == 0 else None
        user = next((item for item in users if isinstance(item, dict) and
                     item.get('username') == username and
                     item.get('companyId') in (self.company_id, -1)), None) if isinstance(users, list) else None
        if user is None:
            raise ApiError(401, 'MES login required')
        if user.get('admin') is not True and user.get('systemAdmin') is not True:
            raise ApiError(403, 'MES admin permission required')
        return username

    def locations(self, params):
        query = urllib.parse.urlencode({'warehouseId': self.warehouse_id, **params})
        try:
            response = fetch_json(self.layout_url + '/locations?' + query)
        except Exception:
            raise ApiError(502, 'Could not load MES locations') from None
        rows = response.get('data') if isinstance(response, dict) and response.get('result') == 0 else None
        if not isinstance(rows, list):
            raise ApiError(502, 'MES locations response was invalid')
        return rows

    @staticmethod
    def eligible(row):
        group = row.get('locationGroup') or {}
        group_type = group.get('locationGroupType') or {}
        return (type(row.get('id')) is int and row['id'] > 0 and
                isinstance(row.get('name'), str) and bool(row['name']) and
                row.get('enabled') is True and group_type.get('virtual') is False)

    def resolve(self, ids):
        if not ids:
            return []
        rows = self.locations({'ids': ','.join(map(str, ids))})
        found = {row['id']: row for row in rows if isinstance(row, dict) and
                 self.eligible(row) and row['id'] in ids}
        if len(found) != len(ids):
            raise ApiError(400, 'Select enabled physical locations in WMEC')
        return [{'id': value, 'name': found[value]['name']} for value in ids]

    def search(self, query):
        if not QUERY_RE.fullmatch(query):
            raise ApiError(400, 'Search with 2–40 location-name characters')
        rows = self.locations({'name': '*' + query + '*', 'maxResultCount': 30})
        return [{'id': row['id'], 'name': row['name']} for row in rows
                if isinstance(row, dict) and self.eligible(row)]

    def read(self):
        try:
            value = json.loads(self.path.read_text()) if self.path.exists() else {'locationIds': []}
        except (OSError, ValueError):
            raise ApiError(500, 'Handoff settings are invalid') from None
        ids = value.get('locationIds') if isinstance(value, dict) else None
        if (not isinstance(ids, list) or len(ids) > MAX_LOCATIONS or
                any(type(item) is not int or item <= 0 for item in ids) or len(set(ids)) != len(ids)):
            raise ApiError(500, 'Handoff settings are invalid')
        revision = hashlib.sha256(canonical({'locationIds': ids}).encode()).hexdigest()
        return ids, revision

    def get(self):
        ids, revision = self.read()
        return {'locationIds': ids, 'locations': self.resolve(ids),
                'revision': revision, 'warehouseId': self.warehouse_id}

    @contextmanager
    def locked(self):
        with open(str(self.path) + '.lock', 'a') as handle:
            fcntl.flock(handle, fcntl.LOCK_EX)
            yield

    def save(self, payload, username):
        if not isinstance(payload, dict) or set(payload) != {'locationIds', 'revision'}:
            raise ApiError(400, 'Location IDs and revision are required')
        ids = payload['locationIds']
        if (not isinstance(ids, list) or len(ids) > MAX_LOCATIONS or
                any(type(item) is not int or item <= 0 for item in ids) or len(set(ids)) != len(ids)):
            raise ApiError(400, 'Select at most 20 unique locations')
        locations = self.resolve(ids)
        with self.locked():
            _, revision = self.read()
            if payload['revision'] != revision:
                raise ApiError(409, 'Settings changed; reload before saving')
            fd, temp_name = tempfile.mkstemp(prefix='.handoff-', dir=str(self.path.parent))
            try:
                os.fchmod(fd, 0o600)
                with os.fdopen(fd, 'w') as stream:
                    stream.write(canonical({'locationIds': ids}) + '\n')
                    stream.flush()
                    os.fsync(stream.fileno())
                os.replace(temp_name, str(self.path))
            finally:
                if os.path.exists(temp_name):
                    os.unlink(temp_name)
        print('Handoff locations updated by MES admin ' + username, flush=True)
        return {'locationIds': ids, 'locations': locations,
                'revision': self.read()[1], 'warehouseId': self.warehouse_id}


class Handler(BaseHTTPRequestHandler):
    def respond(self, status, data=None, message=''):
        body = canonical({'result': 0 if status < 400 else 1,
                          'message': message, 'data': data}).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def handle_request(self, saving=False):
        try:
            parsed = urllib.parse.urlsplit(self.path)
            if parsed.path not in ('/inventory-handoff/locations', '/inventory-handoff/location-options') or (
                    saving and parsed.path != '/inventory-handoff/locations'):
                raise ApiError(404, 'Not found')
            auth = self.headers.get('Authorization', '')
            token = auth[7:] if auth.startswith('Bearer ') else ''
            username = self.server.store.authorize(
                self.headers.get('X-CWMS-Username', ''), token,
                self.headers.get('companyId', ''))
            if saving:
                length = int(self.headers.get('Content-Length', '0'))
                if length < 1 or length > MAX_BODY:
                    raise ApiError(400, 'Invalid request size')
                data = self.server.store.save(json.loads(self.rfile.read(length)), username)
            elif parsed.path == '/inventory-handoff/locations':
                data = self.server.store.get()
            else:
                query = urllib.parse.parse_qs(parsed.query)
                data = self.server.store.search(query.get('q', [''])[0])
            self.respond(200, data)
        except ApiError as error:
            self.respond(error.status, message=str(error))
        except (ValueError, json.JSONDecodeError):
            self.respond(400, message='Invalid JSON request')
        except Exception:
            self.respond(500, message='Inventory handoff settings service failed')

    def do_GET(self):
        self.handle_request()

    def do_PUT(self):
        self.handle_request(saving=True)

    def log_message(self, format, *args):
        # Do not log request headers, tokens, or payloads.
        pass


class Server(ThreadingMixIn, HTTPServer):
    daemon_threads = True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    args = parser.parse_args()
    config = json.loads(args.config.read_text())
    server = Server((config['bind'], config['port']), Handler)
    server.store = Store(config)
    server.serve_forever()


if __name__ == '__main__':
    main()
