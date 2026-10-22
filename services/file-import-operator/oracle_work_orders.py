"""Strict Oracle Work Order CSV conversion; no network or database access."""

import csv
import hashlib
import io
import json
import re
from decimal import Decimal, ROUND_CEILING, localcontext


COLUMNS = (
    'WORK_ORDER_NUMBER', 'FINISHED_ITEM_NUMBER', 'PLANNED_QUANTITY',
    'PO_NUMBER', 'COMPONENT_LINE_NUMBER', 'COMPONENT_ITEM_NUMBER',
    'COMPONENT_QUANTITY',
)
MAX_BYTES = 10 * 1024 * 1024
MAX_ROWS = 10000


class InvalidWorkOrderFile(ValueError):
    pass


def positive_integer(value, field, row_number, adjustments, allow_zero=False):
    # Oracle weight quantities round up at this boundary; MES remains Java Long.
    if not re.fullmatch(r'[0-9]+(?:\.[0-9]+)?', value):
        raise InvalidWorkOrderFile('row %d: %s must be a non-negative decimal number' %
                                   (row_number, field))
    original = Decimal(value)
    if original > 9223372036854775807 or original < 0 or (original == 0 and not allow_zero):
        raise InvalidWorkOrderFile('row %d: %s must be a positive 64-bit integer' %
                                   (row_number, field))
    number = int(original.to_integral_value(rounding=ROUND_CEILING))
    if original != number:
        with localcontext() as context:
            context.prec = max(28, len(value) + 1)
            difference = format(Decimal(number) - original, 'f')
        adjustments.append({'sourceRow': row_number, 'field': field,
                            'originalQuantity': value, 'roundedQuantity': number,
                            'difference': difference, 'rounding': 'CEILING'})
    return number


def convert(content, config, batch_id):
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,100}', batch_id):
        raise InvalidWorkOrderFile('invalid batch ID')
    if len(content) > MAX_BYTES:
        raise InvalidWorkOrderFile('file too large')
    scope = {}
    for key in ('companyCode', 'warehouseName', 'inventoryStatusName'):
        value = config.get(key)
        if not isinstance(value, str) or not value.strip():
            raise InvalidWorkOrderFile('configuration required: ' + key)
        scope[key] = value.strip()
    try:
        text = content.decode('utf-8-sig')
    except UnicodeDecodeError:
        raise InvalidWorkOrderFile('file must be UTF-8') from None
    csv.field_size_limit(MAX_BYTES)
    reader = csv.reader(io.StringIO(text, newline=''), strict=True)
    grouped = {}
    try:
        header = next(reader, [])
        header = [column.strip().upper() for column in header]
        if len(header) != len(COLUMNS) or set(header) != set(COLUMNS):
            raise InvalidWorkOrderFile('expected exactly seven Work Order columns')
        for row_number, values in enumerate(reader, 2):
            if row_number > MAX_ROWS + 1 or len(values) != len(COLUMNS):
                raise InvalidWorkOrderFile('row limit exceeded or incorrect column count')
            row = dict(zip(header, (value.strip() for value in values)))
            for field in ('WORK_ORDER_NUMBER', 'FINISHED_ITEM_NUMBER'):
                if not row[field]:
                    raise InvalidWorkOrderFile('row %d: required field %s' % (row_number, field))
            work_order = row['WORK_ORDER_NUMBER']
            adjustments = []
            positive_integer(row['PLANNED_QUANTITY'], 'PLANNED_QUANTITY',
                             row_number, adjustments)
            header_value = (row['FINISHED_ITEM_NUMBER'],
                            Decimal(row['PLANNED_QUANTITY']), row['PO_NUMBER'])
            group = grouped.setdefault(work_order, {'header': header_value,
                                                    'lines': {}, 'sourceRows': [],
                                                    'skippedZeroComponentRows': [],
                                                    'quantityAdjustments': []})
            if group['header'] != header_value:
                raise InvalidWorkOrderFile('row %d: inconsistent Work Order header' % row_number)
            group['sourceRows'].append(row_number)
            component_quantity = positive_integer(row['COMPONENT_QUANTITY'],
                                                  'COMPONENT_QUANTITY', row_number,
                                                  adjustments, allow_zero=True)
            group['quantityAdjustments'].extend(adjustments)
            if component_quantity == 0:
                group['skippedZeroComponentRows'].append(row_number)
                continue
            for field in ('COMPONENT_LINE_NUMBER', 'COMPONENT_ITEM_NUMBER'):
                if not row[field]:
                    raise InvalidWorkOrderFile('row %d: required field %s' % (row_number, field))
            line_number = row['COMPONENT_LINE_NUMBER']
            if line_number in group['lines']:
                raise InvalidWorkOrderFile('row %d: duplicate component line number' % row_number)
            group['lines'][line_number] = {
                'number': line_number, 'itemName': row['COMPONENT_ITEM_NUMBER'],
                'expectedQuantity': component_quantity,
                'inventoryStatusName': scope['inventoryStatusName'],
                'companyCode': scope['companyCode'], 'warehouseName': scope['warehouseName'],
                'status': 'ATTACHED',
            }
    except csv.Error:
        raise InvalidWorkOrderFile('invalid CSV') from None
    # A valid header-only export means Oracle has no Work Order changes.
    # Keep header validation above so missing/malformed files still fail.
    records = []
    for number, group in grouped.items():
        if not group['lines']:
            raise InvalidWorkOrderFile('Work Order %s has no positive component lines' % number)
        item, quantity, po = group['header']
        payload = {
            'companyCode': scope['companyCode'], 'warehouseName': scope['warehouseName'],
            'number': number, 'itemName': item,
            'expectedQuantity': int(quantity.to_integral_value(rounding=ROUND_CEILING)),
            'status': 'PENDING',
            'workOrderLines': [group['lines'][key] for key in sorted(group['lines'])],
        }
        if po:
            payload['poNumber'] = po
        identity = hashlib.sha256(json.dumps(
            [batch_id, scope['companyCode'], scope['warehouseName'], number],
            ensure_ascii=False, separators=(',', ':')).encode()).hexdigest()
        records.append({'kind': 'work-orders', 'recordId': 'oracle-work-order-' + identity,
                        'workOrderNumber': number, 'sourceRows': group['sourceRows'],
                        'skippedZeroComponentRows': group['skippedZeroComponentRows'],
                        'quantityAdjustments': group['quantityAdjustments'],
                        'payload': payload})
    return {'batchId': batch_id, 'records': records}
