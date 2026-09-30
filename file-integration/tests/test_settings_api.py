import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import settings_api as settings


class SettingsTests(unittest.TestCase):
    def config(self, path):
        return {'oracleItems': {'itemTypeMappingFile': str(path),
                                'inventoryBaseUrl': 'http://inventory.example',
                                'companyId': 1, 'warehouseId': 1},
                'settingsApi': {'authBaseUrl': 'http://auth.example'}}

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
            with patch.object(settings, 'read_json_response', return_value={
                    'result': 0, 'data': {'username': 'alice', 'admin': True}}):
                self.assertEqual(store.authorize('alice', 'valid-token', '1'), 'alice')
                with self.assertRaises(settings.SettingsError):
                    store.authorize('bob', 'valid-token', '1')
                with self.assertRaises(settings.SettingsError):
                    store.authorize('alice', 'valid-token', '2')
            with patch.object(settings, 'read_json_response', return_value={
                    'result': 0, 'data': {'username': 'alice', 'admin': False}}):
                with self.assertRaises(settings.SettingsError) as forbidden:
                    store.authorize('alice', 'valid-token', '1')
                self.assertEqual(forbidden.exception.status, 403)


if __name__ == '__main__':
    unittest.main()
