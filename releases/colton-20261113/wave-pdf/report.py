"""Wave Pick List for the loopback service behind the 4200 preview proxy."""
import base64
from collections import defaultdict
import io
import json
import os
from pathlib import Path
import re
import secrets
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlencode, urlsplit

import requests
from reportlab.graphics.barcode.code128 import Code128
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen.canvas import Canvas

ROOT = Path(os.environ.get('WAVE_PDF_DIR', Path(__file__).resolve().parent / 'files'))
GATEWAY = 'http://apigateway:5555/api/'
PRINTER = os.environ['MES_PRINTER_URL']
TYPE = 'WAVE_PICK_SHEET_BY_LOCATION'
GENERATE = re.compile(r'/api/outbound/waves/([1-9][0-9]{0,15})/pick-report')
FILE = re.compile(r'/api/resource/report-histories/(preview|download|print)/([1-9][0-9]{0,15})/([1-9][0-9]{0,15})/' + TYPE + r'/(WAVE4200_[a-f0-9]{32}\.PDF)')


class ReportError(Exception):
    def __init__(self, status, message):
        self.status, self.message = status, message


def api(path, headers, **params):
    reply = requests.get(GATEWAY + path, headers=headers, params=params,
                         timeout=15, allow_redirects=False)
    if reply.status_code in (401, 403):
        raise ReportError(reply.status_code, 'MES access denied')
    reply.raise_for_status()
    body = reply.json()
    if body.get('result') != 0:
        raise ReportError(502, 'MES could not load report data')
    return body['data']


def wave_record(headers, warehouse, wave_id):
    rows = api('outbound/waves', headers, warehouseId=warehouse, id=wave_id)
    wave = next((row for row in rows if str(row.get('id')) == wave_id
                 and str(row.get('warehouseId')) == warehouse), None)
    if wave is None:
        raise ReportError(404, 'Wave not found in this warehouse')
    return wave


def capacity(package):
    counts = set()
    for entry in (package or {}).get('itemUnitOfMeasures', []):
        uom = entry.get('unitOfMeasure') or {}
        qty = entry.get('quantity')
        if ((uom.get('level') == 'PALLET' or
             (not uom.get('level') and str(uom.get('name', '')).strip().upper() in {'PL', 'PLT', 'PALLET'}))
                and type(qty) is int and qty > 0):
            counts.add(qty)
    return next(iter(counts)) if len(counts) == 1 else None


def breakdown(full, loose, unknown):
    parts = []
    if full:
        parts.append(f'{full:,} pallet' + ('s' if full != 1 else ''))
    if loose:
        parts.append(f'{loose:,} pcs')
    if unknown:
        parts.append(f'{unknown:,} pcs (pack size unknown)')
    return ' + '.join(parts) or '0 pcs'


def collect(headers, warehouse, wave_id):
    wave = wave_record(headers, warehouse, wave_id)
    lines = api('outbound/shipment-lines', headers, warehouseId=warehouse, waveId=wave_id)
    demand = defaultdict(int)
    names = {}
    for line in lines:
        if line.get('status') == 'CANCELLED' or not (line.get('quantity') or 0) > 0:
            continue
        order_line = line.get('orderLine') or {}
        item = order_line.get('item') or {}
        item_id = order_line.get('itemId') or item.get('id')
        if not item_id:
            raise ReportError(502, 'A shipment line has no SKU')
        demand[item_id] += line['quantity']
        names[item_id] = item.get('name') or f'Item #{item_id}'
    picks = api('outbound/picks', headers, warehouseId=warehouse, waveId=wave_id)
    picks = [p for p in picks if p.get('status') != 'CANCELLED' and (p.get('quantity') or 0) > 0]
    inventories = defaultdict(list)
    for offset in range(0, len(picks), 100):
        ids = ','.join(str(p['id']) for p in picks[offset:offset + 100])
        for row in api('inventory/inventories', headers, warehouseId=warehouse, pickIds=ids):
            if not row.get('virtual') and (row.get('quantity') or 0) > 0:
                inventories[row.get('pickId')].append(row)
    groups = defaultdict(lambda: defaultdict(int))
    defaults, packages = {}, {}

    def default_capacity(item_id):
        if item_id not in defaults:
            item = api(f'inventory/items/{item_id}', headers)
            names[item_id] = item.get('name') or names[item_id]
            defaults[item_id] = capacity(item.get('defaultItemPackageType'))
        return defaults[item_id]

    for pick in picks:
        item_id = pick.get('itemId') or (pick.get('item') or {}).get('id')
        if not item_id:
            raise ReportError(502, 'A Pick has no SKU')
        qty = pick['quantity']
        if qty > demand[item_id]:
            raise ReportError(409, 'Pick and shipment quantities do not agree. Refresh the Wave.')
        demand[item_id] -= qty
        names[item_id] = (pick.get('item') or {}).get('name') or names[item_id]
        actual = inventories[pick['id']]
        if actual and sum(r['quantity'] for r in actual) == qty:
            for row in actual:
                pack = row.get('itemPackageType')
                groups[item_id][capacity(pack)] += row['quantity']
        else:
            pack_id = pick.get('itemPackageTypeId')
            if pack_id:
                if pack_id not in packages:
                    packages[pack_id] = capacity(api(f'inventory/itemPackageTypes/{pack_id}', headers))
                per_pallet = packages[pack_id]
            else:
                per_pallet = default_capacity(item_id)
            groups[item_id][per_pallet] += qty
    for item_id, quantity in demand.items():
        if quantity:
            groups[item_id][default_capacity(item_id)] += quantity
    rows = []
    for item_id, quantities in groups.items():
        full = sum(q // p for p, q in quantities.items() if p)
        loose = sum(q % p for p, q in quantities.items() if p)
        rows.append((names[item_id], sum(quantities.values()), full, loose, quantities.get(None, 0)))
    return wave['number'], sorted(rows)


def render(name, rows):
    if not re.fullmatch(r'[ -~]{1,100}', name):
        raise ReportError(400, 'Wave number cannot be encoded as Code 128')
    out = io.BytesIO()
    pdf = Canvas(out, pagesize=A4)
    pdf.setTitle('Wave Pick List - ' + name)
    navy, muted = HexColor('#162238'), HexColor('#4B5B71')
    total = sum(r[1] for r in rows)
    full = sum(r[2] for r in rows)
    pages = [rows[i:i + 11] for i in range(0, len(rows), 11)] or [[]]

    def label(x, y, value, size=11, color=navy, bold=False, right=False):
        pdf.setFillColor(color)
        font = 'Helvetica-Bold' if bold else 'Helvetica'
        pdf.setFont(font, size)
        (pdf.drawRightString if right else pdf.drawString)(x, y, value)

    def box(x, y, w, h, color):
        pdf.setFillColor(HexColor(color))
        pdf.rect(x, y, w, h, fill=1, stroke=0)

    for index, page in enumerate(pages, 1):
        label(42, 790, 'Wave Pick List', 27, bold=True)
        label(42, 760, 'WAVE', 10, muted, True)
        name_size = min(18, 230 * 18 / max(1, pdf.stringWidth(name, 'Helvetica-Bold', 18)))
        label(42, 735, name, name_size, bold=True)
        code = Code128(name, barHeight=47, barWidth=1.12, quiet=True)
        if code.width > 260:
            code = Code128(name, barHeight=47, barWidth=1.12 * 260 / code.width, quiet=True)
        pdf.setFillColor(HexColor('#111111'))
        code.drawOn(pdf, 553 - code.width, 735)
        for x, width, value, title in [
            (42, 168, total, 'Total units'), (221, 161, full, 'Full pallets'),
            (393, 160, len(rows), 'SKUs')
        ]:
            box(x, 618, width, 76, '#F0F4F9')
            label(x + 16, 653, f'{value:,}', 26, bold=True)
            label(x + 16, 633, title, 11, muted)
        label(42, 582, 'SKU summary', 16, bold=True)
        box(42, 534, 511, 30, '#E8EFF7')
        label(56, 545, 'SKU', bold=True)
        label(300, 545, 'Pick qty', bold=True, right=True)
        label(339, 545, 'Pallets / Pieces', bold=True)
        for n, (sku, qty, pallets, loose, unknown) in enumerate(page):
            y = 514 - 32 * n
            if n % 2:
                box(42, y - 11, 511, 32, '#F7F9FC')
            size = min(11, 215 * 11 / max(1, pdf.stringWidth(sku, 'Helvetica', 11)))
            label(56, y, sku, size)
            label(300, y, f'{qty:,}', right=True)
            detail = breakdown(pallets, loose, unknown)
            size = min(11, 205 * 11 / max(1, pdf.stringWidth(detail, 'Helvetica', 11)))
            label(339, y, detail, size)
        if not page:
            label(56, 507, 'No active shipment quantities in this Wave.', color=muted)
        label(42, 67, 'Pallet quantities use the SKU packaging configuration.', 9, muted)
        label(553, 67, f'{index} / {len(pages)}', 9, muted, right=True)
        pdf.showPage()
    pdf.save()
    return out.getvalue()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        # Preview URLs can contain the signed login token.
        pass

    def send(self, status, payload, filename=None):
        if filename:
            body = payload
            kind = 'application/pdf'
        else:
            body = json.dumps(payload).encode()
            kind = 'application/json'
        self.send_response(status)
        self.send_header('Content-Type', kind)
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'no-store')
        if filename:
            self.send_header('Content-Disposition', 'attachment; filename="' + filename + '"')
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        self.handle_report()

    def do_POST(self):
        self.handle_report()

    def handle_report(self):
        path = urlsplit(self.path)
        args = parse_qs(path.query)
        arg = lambda key, fallback='': args.get(key, [fallback])[0]
        generate = GENERATE.fullmatch(path.path)
        file_match = FILE.fullmatch(path.path)
        try:
            if generate and self.command == 'POST':
                warehouse = arg('warehouseId')
                company = self.headers.get('companyId', '')
            elif file_match and ((file_match[1] == 'print') == (self.command == 'POST')):
                action, company, warehouse, filename = file_match.groups()
            else:
                raise ReportError(404, 'Report route not found')
            for value in (warehouse, company):
                if not re.fullmatch(r'[1-9][0-9]{0,15}', value):
                    raise ReportError(400, 'Invalid company or warehouse')
            auth = self.headers.get('Authorization') or ('Bearer ' + arg('token'))
            try:
                payload = auth.removeprefix('Bearer ').split('.')[1]
                identity = json.loads(base64.urlsafe_b64decode(payload + '=' * (-len(payload) % 4)))
                actor = identity['sub']
                if not actor or len(auth) > 10000 or str(identity['companyId']) not in (company, '-1'):
                    raise ValueError
            except (ValueError, IndexError, KeyError, TypeError):
                raise ReportError(401, 'Sign in to MES') from None
            headers = {'Authorization': auth, 'companyId': company, 'warehouseId': warehouse}
            # This validates the original bearer with the MES gateway before using its subject.
            wh = api(f'layout/warehouses/{warehouse}', headers)
            if str(wh.get('companyId') or (wh.get('company') or {}).get('id')) != company:
                raise ReportError(403, 'Warehouse access denied')
            if generate:
                name, rows = collect(headers, warehouse, generate[1])
                pdf = render(name, rows)
                filename = f'WAVE4200_{secrets.token_hex(16)}.PDF'
                (ROOT / filename).write_bytes(pdf)
                (ROOT / (filename + '.json')).write_text(json.dumps({
                    'actor': actor, 'company': company, 'warehouse': warehouse, 'wave': generate[1]
                }))
                self.send(200, {'result': 0, 'data': {
                    'fileName': filename, 'type': TYPE, 'reportOrientation': 'PORTRAIT',
                    'warehouseId': int(warehouse), 'companyId': int(company)
                }})
                return
            file = ROOT / filename
            metadata_file = ROOT / (filename + '.json')
            if not metadata_file.is_file():
                raise ReportError(404, 'Regenerate the Wave PDF')
            metadata = json.loads(metadata_file.read_text())
            if any(metadata[key] != value for key, value in
                   [('actor', actor), ('company', company), ('warehouse', warehouse)]):
                raise ReportError(403, 'Report access denied')
            wave_record(headers, warehouse, metadata['wave'])
            if action in ('preview', 'download'):
                self.send(200, file.read_bytes(), filename)
                return
            printer = arg('printerName')
            copies = arg('copies') or arg('physicalCopyCount')
            if not printer or not copies:
                configs = api('resource/report-printer-configuration', headers,
                              warehouseId=warehouse, type=TYPE, criteriaValue=arg('findPrinterBy'))
                config = next((c for c in configs if (c.get('criteriaValue') or '') == arg('findPrinterBy')), {})
                printer = printer or config.get('printerName') or ''
                copies = copies or str(config.get('copies') or 1)
            if not re.fullmatch(r'[1-9][0-9]{0,2}', copies):
                raise ReportError(400, 'Invalid copy count')
            # Use the existing server printer and the exact stored PDF from Preview.
            params = {'copies': copies, 'collated': arg('collated') == 'true'}
            params['collated'] = str(params['collated']).lower()
            if printer:
                params['printer'] = printer
            response = requests.post(PRINTER + 'printing/pdf', params=params,
                                     files={'file': (filename, file.read_bytes(), 'application/pdf')},
                                     timeout=25, allow_redirects=False)
            response.raise_for_status()
            if not 200 <= response.status_code < 300:
                raise ReportError(502, 'Printer did not accept the PDF')
            self.send(200, {'result': 0, 'data': 'Printed'})
        except ReportError as error:
            self.send(error.status, {'result': error.status, 'message': error.message})
        except (OSError, ValueError, TypeError, KeyError, requests.RequestException) as error:
            print(type(error).__name__, flush=True)
            self.send(502, {'result': 502, 'message': 'Could not process the Wave PDF'})


if __name__ == '__main__':
    os.umask(0o077)
    ROOT.mkdir(mode=0o700, exist_ok=True)
    ThreadingHTTPServer(('127.0.0.1', 18788), Handler).serve_forever()
