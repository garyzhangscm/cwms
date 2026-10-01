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
                'companyId': 1, 'warehouseId': 1, 'adjustmentLocationId': 268}

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
                _, report = job.process(config, execute=True, verify_after=True)
            self.assertEqual(report['records'][0]['status'], 'SKIPPED')
            request.assert_not_called()

    def test_limit_selects_only_lowest_inventory_id(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            first = self.row()
            second = dict(first, id=434965, lpn='L0000083990')
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', return_value=[second, first]), \
                    patch.object(job, 'inventory_count', return_value=2), \
                    patch.object(job, 'request_json') as request:
                _, report = job.process(config, execute=False, limit=1)
            self.assertEqual(report['candidateCount'], 2)
            self.assertEqual(report['selectedCount'], 1)
            self.assertEqual([row['id'] for row in report['records']], [434964])
            request.assert_not_called()

    def test_execute_checks_activity_after_adjustment(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            row = self.row()
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', side_effect=[[row], [row]]), \
                    patch.object(job, 'inventory_count', return_value=1), \
                    patch.object(job, 'request_json', return_value={'unexpected': 'response'}) as request, \
                    patch.object(job, 'adjustment_confirmed', return_value=True) as verified:
                _, report = job.process(config, execute=True, verify_after=True)
            self.assertEqual(report['records'][0]['status'], 'COMPLETED')
            self.assertEqual(request.call_args.kwargs['username'], job.ACTOR)
            self.assertEqual(request.call_args.kwargs['timeout'], 120)
            self.assertIn('documentNumber=EXT-HANDOFF-434964', request.call_args.args[0])
            verified.assert_called_once()

    def test_adjustment_confirmation_requires_virtual_location_and_activity(self):
        moved = dict(self.row(), locationId=268, virtual=True)
        with patch.object(job, 'inventory_rows', return_value=[moved]), \
                patch.object(job, 'activity_confirmed', return_value=True):
            self.assertTrue(job.adjustment_confirmed('http://inventory.test', 1, 434964,
                                                     'L0000083989', 268, 'EXT-HANDOFF-434964'))
        with patch.object(job, 'inventory_rows', return_value=[dict(moved, locationId=13098)]), \
                patch.object(job, 'activity_confirmed') as activity, \
                patch.object(job.time, 'sleep'):
            self.assertFalse(job.adjustment_confirmed('http://inventory.test', 1, 434964,
                                                      'L0000083989', 268, 'EXT-HANDOFF-434964'))
            activity.assert_not_called()

    def test_delete_timeout_is_not_retried_when_state_and_activity_confirm(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            row = self.row()
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', side_effect=[[row], [row]]), \
                    patch.object(job, 'inventory_count', return_value=1), \
                    patch.object(job, 'request_json', side_effect=TimeoutError('timed out')) as delete, \
                    patch.object(job, 'adjustment_confirmed', return_value=True):
                _, report = job.process(config, execute=True, verify_after=True)
            self.assertEqual(report['records'][0]['status'], 'COMPLETED')
            delete.assert_called_once()

    def test_direct_mode_skips_post_adjustment_queries(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            row = self.row()
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', side_effect=[[row], [row]]) as inventory, \
                    patch.object(job, 'inventory_count', return_value=1), \
                    patch.object(job, 'request_json', return_value={'id': 434964}) as delete, \
                    patch.object(job, 'adjustment_confirmed') as verified:
                _, report = job.process(config, execute=True)
            self.assertEqual(report['records'][0]['status'], 'SUBMITTED')
            self.assertEqual(inventory.call_count, 2)
            delete.assert_called_once()
            verified.assert_not_called()

    def test_batch_submits_ids_once_without_post_checks(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            config['submissionMode'] = 'batch'
            rows = [self.row(), dict(self.row(), id=434965)]
            with patch.object(job.Store, 'resolve', return_value=[{'id': 13098, 'name': 'out'}]), \
                    patch.object(job, 'inventory_rows', return_value=rows) as scan, \
                    patch.object(job, 'inventory_count', return_value=2), \
                    patch.object(job, 'request_json', return_value={'result': 0, 'data': 'remove request has been sent'}) as request:
                _, report = job.process(config, execute=True)
            self.assertEqual(report['status'], 'SUBMITTED')
            self.assertEqual([r['status'] for r in report['records']], ['BATCH_ACCEPTED'] * 2)
            request.assert_called_once()
            self.assertEqual(request.call_args.kwargs['data'], '434964,434965')
            self.assertIn('asyncronized=true', request.call_args.args[0])
            scan.assert_called_once()

    def test_batch_timeout_is_not_retried_on_next_run(self):
        with tempfile.TemporaryDirectory() as temp:
            config = self.config(temp)
            config['submissionMode'] = 'batch'
            report_dir = Path(config['reportDirectory'])
            report_dir.mkdir()
            with patch.object(job, 'request_json', side_effect=TimeoutError('timeout')) as request:
                report = {'records': []}
                with self.assertRaises(TimeoutError):
                    job.submit_batch(config, [({'id': 13098}, self.row())], report,
                                     report_dir / 'first-execute.json', True)
                job.write_report(report_dir / 'first-execute.json', report)
                second = {'records': []}
                job.submit_batch(config, [({'id': 13098}, self.row())], second,
                                 report_dir / 'second-execute.json', True)
            request.assert_called_once()
            self.assertEqual(second['records'][0]['status'], 'SKIPPED')


if __name__ == '__main__':
    unittest.main()
