import base64
import json
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch

import settings_api as api


class HandoffTests(unittest.TestCase):
    def store(self, path):
        return api.Store({'stateFile': str(path), 'authBaseUrl': 'http://auth.test',
                          'userBaseUrl': 'http://user.test', 'layoutBaseUrl': 'http://layout.test',
                          'companyId': 1, 'warehouseId': 1})

    def test_save_valid_location_and_reject_duplicate_and_stale_revision(self):
        with tempfile.TemporaryDirectory() as temp:
            store = self.store(Path(temp) / 'locations.json')
            out = {'id': 13098, 'name': 'out', 'enabled': True,
                   'locationGroup': {'locationGroupType': {'virtual': False}}}
            with patch.object(store, 'locations', return_value=[out]):
                before = store.get()
                saved = store.save({'locationIds': [13098],
                                    'revision': before['revision']}, 'admin')
                self.assertEqual(saved['locations'], [{'id': 13098, 'name': 'out'}])
                self.assertEqual(json.loads(store.path.read_text()), {'locationIds': [13098]})
                with self.assertRaises(api.ApiError) as stale:
                    store.save({'locationIds': [], 'revision': before['revision']}, 'admin')
                self.assertEqual(stale.exception.status, 409)
                with self.assertRaises(api.ApiError) as duplicate:
                    store.save({'locationIds': [13098, 13098],
                                'revision': saved['revision']}, 'admin')
                self.assertEqual(duplicate.exception.status, 400)

    def test_reject_disabled_and_virtual_locations(self):
        with tempfile.TemporaryDirectory() as temp:
            store = self.store(Path(temp) / 'locations.json')
            for enabled, virtual in [(False, False), (True, True)]:
                row = {'id': 13098, 'name': 'out', 'enabled': enabled,
                       'locationGroup': {'locationGroupType': {'virtual': virtual}}}
                with patch.object(store, 'locations', return_value=[row]):
                    with self.assertRaises(api.ApiError) as invalid:
                        store.save({'locationIds': [13098],
                                    'revision': store.get()['revision']}, 'admin')
                    self.assertEqual(invalid.exception.status, 400)

    def test_only_matching_admin_can_edit(self):
        with tempfile.TemporaryDirectory() as temp:
            store = self.store(Path(temp) / 'locations.json')
            claims = {'sub': 'alice', 'companyId': 1, 'exp': int(time.time()) + 3600}
            token = 'header.' + base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=') + '.signature'
            with patch.object(api, 'fetch_json', side_effect=[
                    {'result': 0, 'data': 'alice'},
                    {'result': 0, 'data': [{'username': 'alice', 'companyId': 1, 'admin': True}]}]):
                self.assertEqual(store.authorize('alice', token, '1'), 'alice')
            with patch.object(api, 'fetch_json', side_effect=[
                    {'result': 0, 'data': 'alice'},
                    {'result': 0, 'data': [{'username': 'alice', 'companyId': 1, 'admin': False}]}]):
                with self.assertRaises(api.ApiError) as forbidden:
                    store.authorize('alice', token, '1')
                self.assertEqual(forbidden.exception.status, 403)


if __name__ == '__main__':
    unittest.main()
