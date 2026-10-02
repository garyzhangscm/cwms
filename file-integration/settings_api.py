"""Admin-only API for the Item Type mapping used by the FTP importer."""
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

from adapter import canonical


MAX_BODY = 16384
CODE_RE = re.compile(r'[A-Za-z0-9_-]{1,20}')


class SettingsError(Exception):
    def __init__(self, status, message):
        super().__init__(message)
        self.status = status


def read_json_response(url, token=None):
    headers = {'Accept': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=8) as response:
        content = response.read(1024 * 1024 + 1)
    if len(content) > 1024 * 1024:
        raise SettingsError(502, 'MES response was too large')
    return json.loads(content)


def validate_mapping(value, families):
    if (not isinstance(value, dict) or not value or len(value) > 100 or
            any(not isinstance(code, str) or not CODE_RE.fullmatch(code) or
                not isinstance(family, str) or family not in families
                for code, family in value.items())):
        raise SettingsError(400, 'Use unique Item Type codes and existing MES Item Families')
    return value


class SettingsStore:
    def __init__(self, config):
        self.config = config
        self.path = Path(config['oracleItems']['itemTypeMappingFile'])
        self.auth_url = config['settingsApi']['authBaseUrl'].rstrip('/')
        self.user_url = config['settingsApi']['userBaseUrl'].rstrip('/')
        self.inventory_url = config['oracleItems']['inventoryBaseUrl'].rstrip('/')
        self.company_id = config['oracleItems']['companyId']
        self.warehouse_id = config['oracleItems']['warehouseId']

    def authorize(self, username, token, company_id):
        if (not username or not token or len(token) > 4096 or
                str(self.company_id) != company_id):
            print('Settings login rejected: missing headers or company mismatch; '
                  'username_present={}; token_present={}; company_id={}'.format(
                      bool(username), bool(token), company_id), flush=True)
            raise SettingsError(401, 'MES login required')
        try:
            payload = json.loads(base64.urlsafe_b64decode(
                token.split('.')[1] + '==='))
            if (payload.get('sub') != username or
                    payload.get('companyId') not in (self.company_id, -1) or
                    int(payload.get('exp', 0)) <= time.time()):
                raise ValueError('Invalid JWT claims')
        except (IndexError, ValueError, TypeError, binascii.Error):
            print('Settings login rejected: JWT claims invalid', flush=True)
            raise SettingsError(401, 'MES login required') from None
        query = urllib.parse.urlencode({'companyId': self.company_id, 'token': token})
        try:
            response = read_json_response(self.auth_url + '/users/username-by-token?' + query)
        except Exception:
            raise SettingsError(502, 'Could not verify MES login') from None
        verified_name = response.get('data') if isinstance(response, dict) and response.get('result') == 0 else None
        if verified_name != username:
            print('Settings login rejected: auth service did not match username', flush=True)
            raise SettingsError(401, 'MES login required')
        query = urllib.parse.urlencode({'companyId': self.company_id, 'username': username})
        try:
            response = read_json_response(self.user_url + '/users?' + query)
        except Exception:
            raise SettingsError(502, 'Could not verify MES admin permission') from None
        users = response.get('data') if isinstance(response, dict) and response.get('result') == 0 else None
        user = next((item for item in users if isinstance(item, dict) and
                     item.get('username') == username and
                     item.get('companyId') in (self.company_id, -1)), None) if isinstance(users, list) else None
        if user is None:
            print('Settings login rejected: resource user mismatch; '
                  'response_type={}; count={}; exact_name={}; company_ids={}'.format(
                      type(users).__name__, len(users) if isinstance(users, list) else -1,
                      any(isinstance(item, dict) and item.get('username') == username
                          for item in users) if isinstance(users, list) else False,
                      [item.get('companyId') for item in users[:3] if isinstance(item, dict)]
                      if isinstance(users, list) else []), flush=True)
            raise SettingsError(401, 'MES login required')
        if user.get('admin') is not True and user.get('systemAdmin') is not True:
            raise SettingsError(403, 'MES admin permission required')
        return username

    def families(self):
        query = urllib.parse.urlencode({'companyId': self.company_id,
                                        'warehouseId': self.warehouse_id})
        try:
            response = read_json_response(self.inventory_url + '/item-families?' + query)
        except Exception:
            raise SettingsError(502, 'Could not load MES Item Families') from None
        data = response.get('data') if isinstance(response, dict) and response.get('result') == 0 else None
        if not isinstance(data, list):
            raise SettingsError(502, 'MES Item Families response was invalid')
        return sorted({item['name'] for item in data if isinstance(item, dict) and
                       isinstance(item.get('name'), str) and item['name']})

    @contextmanager
    def locked(self):
        with open(str(self.path) + '.lock', 'a') as handle:
            fcntl.flock(handle, fcntl.LOCK_EX)
            yield

    def read(self):
        mapping = json.loads(self.path.read_text())
        if not isinstance(mapping, dict) or not mapping:
            raise SettingsError(500, 'Item Type mapping file is invalid')
        return mapping, hashlib.sha256(canonical(mapping).encode()).hexdigest()

    def get(self):
        mapping, revision = self.read()
        return {'mapping': mapping, 'revision': revision, 'families': self.families()}

    def save(self, payload, username):
        if not isinstance(payload, dict) or set(payload) != {'mapping', 'revision'}:
            raise SettingsError(400, 'Mapping and revision are required')
        families = self.families()
        mapping = validate_mapping(payload['mapping'], families)
        with self.locked():
            _, revision = self.read()
            if payload['revision'] != revision:
                raise SettingsError(409, 'Settings changed; reload before saving')
            fd, temp_name = tempfile.mkstemp(prefix='.item-type-', dir=str(self.path.parent))
            try:
                os.fchmod(fd, 0o600)
                with os.fdopen(fd, 'w') as stream:
                    stream.write(canonical(mapping) + '\n')
                    stream.flush()
                    os.fsync(stream.fileno())
                os.replace(temp_name, str(self.path))
            finally:
                if os.path.exists(temp_name):
                    os.unlink(temp_name)
        print('Item Type mapping updated by MES admin ' + username, flush=True)
        return self.get()


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
            if self.path != '/integration-settings/item-types':
                raise SettingsError(404, 'Not found')
            auth = self.headers.get('Authorization', '')
            token = auth[7:] if auth.startswith('Bearer ') else ''
            username = self.server.store.authorize(
                self.headers.get('X-CWMS-Username', ''), token,
                self.headers.get('companyId', ''))
            if saving:
                length = int(self.headers.get('Content-Length', '0'))
                if length < 1 or length > MAX_BODY:
                    raise SettingsError(400, 'Invalid request size')
                payload = json.loads(self.rfile.read(length))
                data = self.server.store.save(payload, username)
            else:
                data = self.server.store.get()
            self.respond(200, data)
        except SettingsError as error:
            self.respond(error.status, message=str(error))
        except (ValueError, json.JSONDecodeError):
            self.respond(400, message='Invalid JSON request')
        except Exception:
            self.respond(500, message='Settings service failed')

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
    server = Server((config['settingsApi']['bind'], config['settingsApi']['port']), Handler)
    server.store = SettingsStore(config)
    server.serve_forever()


if __name__ == '__main__':
    main()
