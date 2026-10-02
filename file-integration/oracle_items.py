"""Offline Oracle five-column item preview. Never submits to MES."""
import argparse
import csv
import hashlib
import io
import json
import os
import re
from pathlib import Path

from adapter import InvalidFile, MAX_BYTES, MAX_ROWS, canonical, typed


COLUMNS = ('item_number', 'item_description', 'item_type',
           'pieces_per_carton', 'pieces_per_pallet')
HEADER_ALIASES = {'segment1': 'item_number', 'description': 'item_description'}
UNIT_FLAGS = {'defaultForInboundReceiving', 'defaultForWorkOrderReceiving',
              'trackingLpn', 'defaultForDisplay', 'caseFlag'}


def unit_options(config):
    options = config.get('unitOptions')
    if options is None:
        return {}
    if not isinstance(options, dict) or set(options) != {'piece', 'carton', 'pallet'}:
        raise InvalidFile('unitOptions requires piece, carton and pallet')
    for values in options.values():
        if (not isinstance(values, dict) or set(values) != UNIT_FLAGS or
                any(type(value) is not bool for value in values.values())):
            raise InvalidFile('each unitOptions entry requires all five boolean flags')
    return options


def unit_measurements(config):
    values = config.get('unitMeasurements')
    if values is None:
        return {}
    roles = {'piece', 'carton', 'pallet'}
    fields = {'length', 'width', 'height', 'weight'}
    if not isinstance(values, dict) or set(values) != roles:
        raise InvalidFile('unitMeasurements requires piece, carton and pallet')
    result = {}
    for role, dimensions in values.items():
        if not isinstance(dimensions, dict) or set(dimensions) != fields:
            raise InvalidFile('each measurement entry requires length, width, height and weight')
        result[role] = {}
        for field, value in dimensions.items():
            if type(value) not in (int, float):
                raise InvalidFile('measurements must be JSON numbers')
            number = typed(field, str(value))
            if number <= 0:
                raise InvalidFile('measurements must be positive')
            result[role][field] = number
    return result


def quantity(value, field, defaults):
    value = value.strip()
    if not value:
        defaults.append({'field': field, 'reason': 'empty', 'value': 1})
        return 1
    number = typed('quantity', value)
    if number == 0:
        defaults.append({'field': field, 'reason': 'zero', 'value': 1})
        return 1
    return number


def convert(content, config, batch_id):
    options = unit_options(config)
    measurements = unit_measurements(config)
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,100}', batch_id):
        raise InvalidFile('invalid batch ID')
    if len(content) > MAX_BYTES:
        raise InvalidFile('file too large')
    scope = {}
    for key in ('companyCode', 'warehouseName'):
        value = config.get(key)
        if not isinstance(value, str) or not value.strip():
            raise InvalidFile('configuration required: ' + key)
        scope[key] = value.strip()
    if config.get('clientName'):
        scope['clientName'] = config['clientName']
    families = config.get('itemTypeMapping')
    if not isinstance(families, dict) or not families or any(
            not isinstance(k, str) or not isinstance(v, str) or not v.strip()
            for k, v in families.items()):
        raise InvalidFile('itemTypeMapping must map text codes to family names')
    carton_unit = config.get('cartonUnit', 'CTN')
    if not isinstance(carton_unit, str) or not carton_unit.strip() or carton_unit.upper() in ('PCS', 'PL'):
        raise InvalidFile('cartonUnit must differ from PCS and PL')
    try:
        text = content.decode('utf-8-sig')
    except UnicodeDecodeError:
        raise InvalidFile('file must be UTF-8') from None
    csv.field_size_limit(MAX_BYTES)
    reader = csv.reader(io.StringIO(text, newline=''), strict=True)
    records, seen = [], set()
    try:
        header = next(reader, [])
        header = [x.strip().lower().replace(' ', '_') for x in header]
        header = [HEADER_ALIASES.get(x, x) for x in header]
        if len(header) != len(COLUMNS) or set(header) != set(COLUMNS):
            raise InvalidFile('exactly the five Oracle item columns are required')
        for row_index, values in enumerate(reader, 2):
            if len(records) >= MAX_ROWS or len(values) != len(COLUMNS):
                raise InvalidFile('row limit exceeded or incorrect column count')
            row = dict(zip(header, (v.strip() for v in values)))
            for field in ('item_number', 'item_description', 'item_type'):
                if not row[field]:
                    raise InvalidFile('row %d: required field %s' % (row_index, field))
            number = row['item_number']
            if number in seen:
                raise InvalidFile('row %d: duplicate item number' % row_index)
            seen.add(number)
            if row['item_type'] not in families:
                raise InvalidFile('row %d: unmapped item type; preserve leading zeros' % row_index)
            defaults = []
            carton = quantity(row['pieces_per_carton'], 'pieces_per_carton', defaults)
            pallet = quantity(row['pieces_per_pallet'], 'pieces_per_pallet', defaults)
            context = {k: scope[k] for k in ('companyCode', 'warehouseName')}
            units = [{'unitOfMeasureName': unit, 'quantity': count, **context,
                      **options.get(role, {}), **measurements.get(role, {})}
                     for role, unit, count in [('piece', 'PCS', 1),
                         ('carton', carton_unit.strip(), carton), ('pallet', 'PL', pallet)]]
            payload = {
                **scope, 'name': number, 'description': row['item_description'],
                'itemFamily': {**context, 'name': families[row['item_type']],
                               'description': families[row['item_type']]},
                'itemPackageTypes': [{**scope, 'itemName': number, 'name': 'Main',
                                      'description': 'Main', 'itemUnitOfMeasures': units}],
            }
            # Batch + scope + item, independent of row order and payload. A changed
            # payload under the same event identity must be rejected by the ledger.
            identity = hashlib.sha256(canonical([batch_id, scope, number]).encode()).hexdigest()
            records.append({'kind': 'items', 'recordId': 'oracle-item-' + identity,
                            'sourceRow': row_index, 'defaultsApplied': defaults,
                            'payload': payload})
    except csv.Error:
        raise InvalidFile('invalid CSV') from None
    return {'mode': 'OFFLINE_PREVIEW_ONLY', 'batchId': batch_id, 'records': records}


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('file', type=Path)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--batch-id', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    with args.file.open('rb') as f:
        content = f.read(MAX_BYTES + 1)
    result = convert(content, json.loads(args.config.read_text()), args.batch_id)
    # Exclusive output: never overwrite source data or an earlier preview.
    with args.output.open('x', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
        f.write('\n')
    print(json.dumps({'mode': result['mode'], 'records': len(result['records']),
                      'defaultedFields': sum(len(r['defaultsApplied']) for r in result['records'])}))


if __name__ == '__main__':
    main()
