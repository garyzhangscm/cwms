import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import run_handoff as job


class HandoffRunTests(unittest.TestCase):
    def config(self, temp):
        path = Path(temp) / 'locations.json'
        path.write_text('{"locationIds":[13098]}')
        return {'stateFile': str(path), 'reportDirectory': str(Path(temp) / 'reports'),
                'authBaseUrl': 'http://auth.test', 'userBaseUrl': 'http://user.test',
                'layoutBaseUrl': 'http://layout.test', 'inventoryBaseUrl': 'http://inventory.test',
                'companyId': 1, 'warehouseId': 1}

    def row(self):
        return {'id': 434964, 'lpn': 'L0000083989', 'locationId': 13098,
                'warehouseId': 1, 'quantity': 120, 'virtual': False,
                'lockedForAdjust': False, 'locks': [], 'allocatedByPickId': None,
                'pickNumber': None}

    def test_dry_run_never_adjusts_inventory(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', return_value=[self.row()]), \
                    patch.object(job, 'inventory_count', return_value=1), \
                    patch.object(job, 'request_json') as request:
                path, report = job.process(config, execute=False)
            self.assertEqual(report['status'], 'COMPLETED')
            self.assertEqual(report['records'][0]['status'], 'READY')
            self.assertTrue(path.exists())
            request.assert_not_called()

    def test_locked_inventory_is_skipped(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            row = dict(self.row(), lockedForAdjust=True)
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', return_value=[row]), \
                    patch.object(job, 'inventory_count', return_value=1), \
                    patch.object(job, 'request_json') as request:
                _, report = job.process(config, execute=True)
            self.assertEqual(report['records'][0]['status'], 'SKIPPED')
            request.assert_not_called()

    def test_execute_checks_activity_after_adjustment(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            row = self.row()
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', side_effect=[[row], [row]]), \
                    patch.object(job, 'inventory_count', return_value=1), \
                    patch.object(job, 'request_json', return_value={'id': 434964, 'locationId': 268}) as request, \
                    patch.object(job, 'activity_confirmed', return_value=True) as activity:
                _, report = job.process(config, execute=True)
            self.assertEqual(report['records'][0]['status'], 'COMPLETED')
            self.assertEqual(request.call_args.kwargs['username'], job.ACTOR)
            self.assertIn('documentNumber=EXT-HANDOFF-434964', request.call_args.args[0])
            activity.assert_called_once()


if __name__ == '__main__':
    unittest.main()
