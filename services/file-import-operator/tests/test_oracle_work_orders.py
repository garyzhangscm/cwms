import unittest
from pathlib import Path

from oracle_work_orders import convert, InvalidWorkOrderFile


SAMPLE = (Path(__file__).resolve().parents[1] / 'examples' /
          'int_workorder_demo001.csv').read_bytes()
CONFIG = {'companyCode': '20901', 'warehouseName': 'WMEC',
          'inventoryStatusName': 'TEST_ONLY_STATUS'}


class WorkOrderConversionTests(unittest.TestCase):
    def test_two_lines_make_one_order_with_mes_status_and_scope(self):
        result = convert(SAMPLE, CONFIG, 'demo001')
        self.assertEqual(len(result['records']), 1)
        record = result['records'][0]
        self.assertEqual(record['sourceRows'], [2, 3])
        self.assertEqual(record['payload']['status'], 'PENDING')
        self.assertEqual(record['payload']['itemName'], 'FG-001')
        self.assertEqual(record['payload']['expectedQuantity'], 10)
        self.assertEqual([line['expectedQuantity'] for line in record['payload']['workOrderLines']],
                         [20, 3])
        self.assertEqual([line['status'] for line in record['payload']['workOrderLines']],
                         ['ATTACHED', 'ATTACHED'])
        self.assertEqual(record['payload']['workOrderLines'][0]['inventoryStatusName'],
                         'TEST_ONLY_STATUS')

    def test_accepts_same_columns_in_oracle_export_order(self):
        lines = SAMPLE.decode().splitlines()
        columns = lines[0].split(',')
        reordered = [columns[i] for i in (0, 1, 2, 4, 5, 6, 3)]
        source = '\n'.join([','.join(reordered)] +
                           [','.join([values.split(',')[i] for i in (0, 1, 2, 4, 5, 6, 3)])
                            for values in lines[1:]]) + '\n'
        self.assertEqual(convert(source.encode(), CONFIG, 'demo001')['records'],
                         convert(SAMPLE, CONFIG, 'demo001')['records'])

    def test_decimal_weights_round_up_and_report_exact_difference(self):
        source = SAMPLE.replace(b',10,', b',33293.5794,').replace(b',RM-001,20', b',RM-001,3682.62')
        record = convert(source, CONFIG, 'demo001')['records'][0]
        self.assertEqual(record['payload']['expectedQuantity'], 33294)
        self.assertEqual(record['payload']['workOrderLines'][0]['expectedQuantity'], 3683)
        adjustments = record['quantityAdjustments']
        self.assertEqual(adjustments[0]['originalQuantity'], '33293.5794')
        self.assertEqual(adjustments[0]['difference'], '0.4206')
        self.assertEqual(adjustments[1]['difference'], '0.38')

    def test_exact_integers_and_large_decimal_boundary_preserve_precision(self):
        for quantity, rounded in ((b'10.000', 10),
                                  (b'9223372036854775806.0001', 9223372036854775807),
                                  (b'0.000000000000000000000000000001', 1)):
            record = convert(SAMPLE.replace(b',10,', b',' + quantity + b','), CONFIG, 'demo001')['records'][0]
            self.assertEqual(record['payload']['expectedQuantity'], rounded)
        self.assertEqual(convert(SAMPLE, CONFIG, 'demo001')['records'][0]['quantityAdjustments'], [])

    def test_invalid_decimal_and_overflow_are_rejected(self):
        for value in (b'-0.1', b'NaN', b'Infinity', b'1e2', b'0', b'9223372036854775807.1'):
            with self.subTest(value=value), self.assertRaises(InvalidWorkOrderFile):
                convert(SAMPLE.replace(b',10,', b',' + value + b','), CONFIG, 'demo001')

    def test_conflicting_original_quantities_are_not_hidden_by_rounding(self):
        source = SAMPLE.replace(b',10,', b',10.1,', 1).replace(b',10,', b',10.2,', 1)
        with self.assertRaisesRegex(InvalidWorkOrderFile, 'inconsistent Work Order header'):
            convert(source, CONFIG, 'demo001')

    def test_zero_quantity_component_is_skipped_and_recorded(self):
        source = SAMPLE.replace(b',RM-001,20', b',RM-001,0')
        record = convert(source, CONFIG, 'demo001')['records'][0]
        self.assertEqual(record['sourceRows'], [2, 3])
        self.assertEqual(record['skippedZeroComponentRows'], [2])
        self.assertEqual([line['itemName'] for line in record['payload']['workOrderLines']],
                         ['RM-002'])
        with self.assertRaisesRegex(InvalidWorkOrderFile, 'no positive component lines'):
            convert(source.replace(b',RM-002,3', b',RM-002,0'), CONFIG, 'demo001')

    def test_conflict_duplicate_and_invalid_line_rejected_before_submission(self):
        for data in (SAMPLE.replace(b'FG-001,10,PO-001,2', b'FG-002,10,PO-001,2'),
                     SAMPLE.replace(b',2,RM-002,3', b',1,RM-002,3'),
                     SAMPLE.replace(b',RM-001,20', b',RM-001,-1'),
                     SAMPLE + b'WO-TEST-002,FG-001,1,,1,RM-001\n'):
            with self.subTest(data=data[-50:]), self.assertRaises(InvalidWorkOrderFile):
                convert(data, CONFIG, 'demo001')

    def test_batch_identity_stable_when_rows_reordered(self):
        lines = SAMPLE.splitlines(keepends=True)
        original = convert(SAMPLE, CONFIG, 'demo001')['records'][0]
        reordered = convert(b''.join([lines[0], lines[2], lines[1]]), CONFIG,
                            'demo001')['records'][0]
        self.assertEqual(original['recordId'], reordered['recordId'])
        self.assertEqual(original['payload'], reordered['payload'])


if __name__ == '__main__':
    unittest.main()
