import json
import base64
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch

import settings_api as settings


class SettingsTests(unittest.TestCase):
    def config(self, path):
        return {'oracleItems': {'itemTypeMappingFile': str(path),
                                'inventoryBaseUrl': 'http://inventory.example',
                                'companyId': 1, 'warehouseId': 1},
                'settingsApi': {'authBaseUrl': 'http://auth.example',
                                'userBaseUrl': 'http://resource.example'}}

    def jwt(self, username='alice', company_id=1, expires=None):
        claims = {'sub': username, 'companyId': company_id,
                  'exp': expires if expires is not None else int(time.time()) + 3600}
        return 'header.' + base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=') + '.signature'

    def test_mapping_save_checks_family_and_revision(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'mapping.json'
            path.write_text('{"01":"Finish Good"}')
            store = settings.SettingsStore(self.config(path))
            with patch.object(store, 'families', return_value=['Finish Good', 'Raw Material']):
                original = store.get()
                changed = store.save({'revision': original['revision'],
                                      'mapping': {'01': 'Finish Good',
                                                  '02': 'Raw Material'}}, 'admin')
                self.assertEqual(changed['mapping']['02'], 'Raw Material')
                self.assertNotEqual(changed['revision'], original['revision'])
                self.assertEqual(json.loads(path.read_text())['02'], 'Raw Material')
                with self.assertRaises(settings.SettingsError) as conflict:
                    store.save({'revision': original['revision'],
                                'mapping': {'01': 'Finish Good'}}, 'admin')
                self.assertEqual(conflict.exception.status, 409)
                with self.assertRaises(settings.SettingsError) as invalid:
                    store.save({'revision': changed['revision'],
                                'mapping': {'01': 'Unknown Family'}}, 'admin')
                self.assertEqual(invalid.exception.status, 400)

    def test_only_matching_admin_token_can_edit(self):
        with tempfile.TemporaryDirectory() as temp:
            store = settings.SettingsStore(self.config(Path(temp) / 'mapping.json'))
            with patch.object(settings, 'read_json_response', side_effect=[
                    {'result': 0, 'data': 'alice'},
                    {'result': 0, 'data': [{'username': 'alice', 'companyId': 1, 'admin': True}]}]) as call:
                self.assertEqual(store.authorize('alice', self.jwt(), '1'), 'alice')
                self.assertIn('/users/username-by-token?', call.call_args_list[0].args[0])
                self.assertIn('/users?', call.call_args_list[1].args[0])
                with self.assertRaises(settings.SettingsError):
                    store.authorize('bob', self.jwt(), '1')
                with self.assertRaises(settings.SettingsError):
                    store.authorize('alice', self.jwt(), '2')
                with self.assertRaises(settings.SettingsError):
                    store.authorize('alice', self.jwt(expires=1), '1')
            with patch.object(settings, 'read_json_response', side_effect=[
                    {'result': 0, 'data': 'alice'},
                    {'result': 0, 'data': [{'username': 'alice', 'companyId': 1, 'admin': False}]}]):
                with self.assertRaises(settings.SettingsError) as forbidden:
                    store.authorize('alice', self.jwt(), '1')
                self.assertEqual(forbidden.exception.status, 403)
            with patch.object(settings, 'read_json_response', side_effect=[
                    {'result': 0, 'data': 'alice'},
                    {'result': 0, 'data': [{'username': 'alice', 'companyId': -1,
                                           'systemAdmin': True}]}]):
                self.assertEqual(store.authorize('alice', self.jwt(company_id=-1), '1'), 'alice')


if __name__ == '__main__':
    unittest.main()
