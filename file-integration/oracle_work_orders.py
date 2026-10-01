"""Strict Oracle Work Order CSV conversion; no network or database access."""

import csv
import hashlib
import io
import json
import re


COLUMNS = (
    'WORK_ORDER_NUMBER', 'FINISHED_ITEM_NUMBER', 'PLANNED_QUANTITY',
    'PO_NUMBER', 'COMPONENT_LINE_NUMBER', 'COMPONENT_ITEM_NUMBER',
    'COMPONENT_QUANTITY',
)
MAX_BYTES = 10 * 1024 * 1024
MAX_ROWS = 10000


class InvalidWorkOrderFile(ValueError):
    pass


def positive_integer(value, field, row_number):
    # MES stores Work Order and line quantities as Java Long. Never round a decimal.
    if not re.fullmatch(r'[0-9]+', value):
        raise InvalidWorkOrderFile('row %d: %s must be a whole number' % (row_number, field))
    number = int(value)
    if not 0 < number <= 9223372036854775807:
        raise InvalidWorkOrderFile('row %d: %s must be a positive 64-bit integer' %
                                   (row_number, field))
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
            for field in ('WORK_ORDER_NUMBER', 'FINISHED_ITEM_NUMBER',
                          'COMPONENT_LINE_NUMBER', 'COMPONENT_ITEM_NUMBER'):
                if not row[field]:
                    raise InvalidWorkOrderFile('row %d: required field %s' % (row_number, field))
            work_order = row['WORK_ORDER_NUMBER']
            header_value = (row['FINISHED_ITEM_NUMBER'],
                            positive_integer(row['PLANNED_QUANTITY'],
                                             'PLANNED_QUANTITY', row_number), row['PO_NUMBER'])
            group = grouped.setdefault(work_order, {'header': header_value,
                                                    'lines': {}, 'sourceRows': []})
            if group['header'] != header_value:
                raise InvalidWorkOrderFile('row %d: inconsistent Work Order header' % row_number)
            line_number = row['COMPONENT_LINE_NUMBER']
            if line_number in group['lines']:
                raise InvalidWorkOrderFile('row %d: duplicate component line number' % row_number)
            group['lines'][line_number] = {
                'number': line_number, 'itemName': row['COMPONENT_ITEM_NUMBER'],
                'expectedQuantity': positive_integer(row['COMPONENT_QUANTITY'],
                                                     'COMPONENT_QUANTITY', row_number),
                'inventoryStatusName': scope['inventoryStatusName'],
                'companyCode': scope['companyCode'], 'warehouseName': scope['warehouseName'],
                'status': 'ATTACHED',
            }
            group['sourceRows'].append(row_number)
    except csv.Error:
        raise InvalidWorkOrderFile('invalid CSV') from None
    if not grouped:
        raise InvalidWorkOrderFile('empty Work Order file')
    records = []
    for number, group in grouped.items():
        item, quantity, po = group['header']
        payload = {
            'companyCode': scope['companyCode'], 'warehouseName': scope['warehouseName'],
            'number': number, 'itemName': item, 'expectedQuantity': quantity,
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
                        'payload': payload})
    return {'batchId': batch_id, 'records': records}
