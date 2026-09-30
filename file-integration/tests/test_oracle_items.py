import csv
import io
import unittest
import json
from pathlib import Path

from adapter import InvalidFile
from oracle_items import COLUMNS, UNIT_FLAGS, convert

CONFIG = {'companyCode': 'C', 'warehouseName': 'W', 'itemTypeMapping': {'01': 'Finish Good'}}


def data(rows, header=COLUMNS):
    out = io.StringIO(newline='')
    writer = csv.writer(out)
    writer.writerow(header)
    writer.writerows(rows)
    return out.getvalue().encode('utf-8')


class OracleItemsTests(unittest.TestCase):
    def test_measurements_are_explicit_and_role_specific(self):
        measures = {role: dict(length=i, width=i, height=i, weight=i)
                    for i, role in enumerate(('piece', 'carton', 'pallet'), 1)}
        r = convert(data([['A', 'D', '01', '6', '450']]),
                    {**CONFIG, 'unitMeasurements': measures}, 'b')['records'][0]
        units = r['payload']['itemPackageTypes'][0]['itemUnitOfMeasures']
        self.assertEqual([u['height'] for u in units], [1.0, 2.0, 3.0])
        self.assertEqual([u['quantity'] for u in units], [1, 6, 450])
        for value in (0, -1, float('nan'), True, '1'):
            bad = {**measures, 'pallet': {**measures['pallet'], 'height': value}}
            with self.assertRaises(InvalidFile):
                convert(data([['A', 'D', '01', '6', '450']]),
                        {**CONFIG, 'unitMeasurements': bad}, 'b')

    def test_approved_unit_options(self):
        config = json.loads((Path(__file__).resolve().parents[1] / 'item-mapping.example.json').read_text())
        r = convert(data([['TEST-A', 'D', '01', '', '0']]), config, 'b')['records'][0]
        units = r['payload']['itemPackageTypes'][0]['itemUnitOfMeasures']
        self.assertEqual([u['unitOfMeasureName'] for u in units], ['PCS', 'CS', 'PL'])
        for index, unit in enumerate(units):
            for key in UNIT_FLAGS:
                self.assertIs(unit[key], index == 2)
            self.assertEqual(unit['quantity'], 1)

    def test_formal_item_example_maps_type_and_measurements(self):
        config = json.loads((Path(__file__).resolve().parents[1] /
                             'config.oracle-items.example.json').read_text())
        mapping = config['oracleItems']['mapping']
        mapping.update(companyCode='C', warehouseName='W')
        payload = convert(data([['TEST-A', 'D', '01', '6', '450']]), mapping, 'b')['records'][0]['payload']
        self.assertEqual(payload['itemFamily']['name'], 'Finish Good')
        units = payload['itemPackageTypes'][0]['itemUnitOfMeasures']
        self.assertEqual([unit['quantity'] for unit in units], [1, 6, 450])
        for unit in units:
            self.assertEqual([unit[field] for field in ('length', 'width', 'height', 'weight')],
                             [1.0, 1.0, 1.0, 1.0])

    def test_invalid_or_partial_unit_options_rejected(self):
        valid = {role: {flag: False for flag in UNIT_FLAGS} for role in ('piece', 'carton', 'pallet')}
        for bad in ({}, {'piece': {}}, {**valid, 'pallet': {**valid['pallet'], 'caseFlag': 'false'}}):
            with self.assertRaises(InvalidFile):
                convert(data([['A', 'D', '01', '1', '1']]), {**CONFIG, 'unitOptions': bad}, 'b')

    def test_oracle_native_headers(self):
        rows = [['TEST-ITEM-ALIAS', 'Test description', '01', '', '0']]
        native = ['SEGMENT1', 'DESCRIPTION', 'ITEM_TYPE', 'PIECES_PER_CARTON', 'PIECES_PER_PALLET']
        self.assertEqual(convert(data(rows, native), CONFIG, 'b'),
                         convert(data(rows), CONFIG, 'b'))

    def test_alias_collision_is_rejected(self):
        headers = ['SEGMENT1', 'item_number', 'ITEM_TYPE', 'PIECES_PER_CARTON', 'PIECES_PER_PALLET']
        with self.assertRaises(InvalidFile):
            convert(data([['A', 'D', '01', '1', '1']], headers), CONFIG, 'b')

    def test_mapping_and_nested_package(self):
        record = convert(data([['A', 'Item A', '01', '12', '360']]), CONFIG, 'b1')['records'][0]
        payload = record['payload']
        self.assertEqual(payload['itemFamily']['name'], 'Finish Good')
        self.assertEqual(payload['itemFamily']['description'], 'Finish Good')
        package = payload['itemPackageTypes'][0]
        self.assertEqual(package['name'], 'Main')
        self.assertEqual([(u['unitOfMeasureName'], u['quantity']) for u in package['itemUnitOfMeasures']],
                         [('PCS', 1), ('CTN', 12), ('PL', 360)])
        self.assertEqual(record['defaultsApplied'], [])
        for field in ('unitCost', 'nonInventoryItem', 'id'):
            self.assertNotIn(field, payload)
        self.assertNotIn('trackingLpn', package['itemUnitOfMeasures'][0])

    def test_all_empty_and_zero_combinations(self):
        for carton in ('', '0', '  '):
            for pallet in ('', '0', '  '):
                r = convert(data([['A', 'D', '01', carton, pallet]]), CONFIG, 'b')['records'][0]
                self.assertEqual([u['quantity'] for u in r['payload']['itemPackageTypes'][0]['itemUnitOfMeasures']], [1, 1, 1])
                self.assertEqual(len(r['defaultsApplied']), 2)

    def test_invalid_quantities_fail(self):
        for value in ('-1', '1.5', 'text', '2147483648'):
            for index in (3, 4):
                row = ['A', 'D', '01', '1', '1']
                row[index] = value
                with self.assertRaises(InvalidFile):
                    convert(data([row]), CONFIG, 'b')

    def test_unknown_type_and_lost_leading_zero_fail(self):
        for code in ('1', '02', ''):
            with self.assertRaises(InvalidFile):
                convert(data([['A', 'D', code, '1', '1']]), CONFIG, 'b')

    def test_configurable_type_and_carton_unit(self):
        config = {**CONFIG, 'itemTypeMapping': {'02': 'Raw Material'}, 'cartonUnit': 'CS'}
        p = convert(data([['A', 'D', '02', '12', '360']]), config, 'b')['records'][0]['payload']
        self.assertEqual(p['itemFamily']['name'], 'Raw Material')
        self.assertEqual(p['itemPackageTypes'][0]['itemUnitOfMeasures'][1]['unitOfMeasureName'], 'CS')

    def test_duplicate_item_and_bad_shape_fail(self):
        row = ['A', 'D', '01', '1', '1']
        for rows in ([row, row], [row[:-1]], []):
            with self.assertRaises(InvalidFile):
                convert(data(rows), CONFIG, 'b')

    def test_identity_stable_on_reorder_and_changed_payload(self):
        rows = [['A', 'D', '01', '1', '1'], ['B', 'D', '01', '2', '2']]
        a = convert(data(rows), CONFIG, 'b')['records']
        b = convert(data(rows[::-1]), CONFIG, 'b')['records']
        self.assertEqual(a[0]['recordId'], b[1]['recordId'])
        rows[0][4] = '360'
        c = convert(data(rows), CONFIG, 'b')['records']
        self.assertEqual(a[0]['recordId'], c[0]['recordId'])
        d = convert(data(rows), CONFIG, 'new-batch')['records']
        self.assertNotEqual(c[0]['recordId'], d[0]['recordId'])

    def test_space_headers_bom_and_quoted_description(self):
        raw = data([['A', 'Desc, with\nnewline', '01', '1', '1']], [s.replace('_', ' ') for s in COLUMNS])
        r = convert(b'\xef\xbb\xbf' + raw, CONFIG, 'b')['records'][0]
        self.assertEqual(r['payload']['description'], 'Desc, with\nnewline')


if __name__ == '__main__':
    unittest.main()
