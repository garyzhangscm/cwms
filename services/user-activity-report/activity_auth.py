"""Reuse existing gateway identity verification; require Inventory Activity access."""
import hashlib,base64,json,os,time
from urllib.parse import urlencode
from report_api import ReportError

def authorize(header, company_id, warehouse_id):
    import requests
    if not header or not header.startswith('Bearer ') or len(header) > 10000:
        raise ReportError(401, 'Sign in to MES')
    try:
        payload=header[7:].split('.')[1]
        claims=json.loads(base64.urlsafe_b64decode(payload+'='*(-len(payload)%4)))
        if (not isinstance(claims.get('sub'),str) or not claims['sub'].strip()
                or type(claims.get('companyId')) is not int or claims['companyId'] not in (company_id,-1)
                or type(claims.get('exp')) not in (int,float) or claims['exp']<=time.time()):
            raise ValueError()
    except (ValueError,KeyError,TypeError,IndexError):
        raise ReportError(401, 'Sign in with a valid account') from None
    gateway = os.environ.get('QC_GATEWAY_URL', '').rstrip('/')
    if not gateway:
        raise ReportError(503, 'Report authentication is not configured')
    query = urlencode(dict(companyId=company_id, warehouseId=warehouse_id))
    response = requests.get(gateway + '/resource/site-information?' + query,
                            # The gateway verifies the unchanged bearer token. Legacy Resource
                            # reads the username header; derive it only from that same token,
                            # never from a caller-supplied username. Recheck identity below.
                            headers={'Authorization': header, 'username': claims['sub'],
                                     'companyId': str(company_id), 'warehouseId': str(warehouse_id)},
                            timeout=10, allow_redirects=False)
    if response.status_code != 200:
        raise ReportError(403, 'MES access could not be verified')
    body = response.json()
    info = body.get('data') if body.get('result') == 0 else None
    user = info.get('user') if isinstance(info, dict) else None
    if not isinstance(user, dict) or not user.get('id') or not user.get('username'):
        raise ReportError(403, 'MES access could not be verified')
    # Claims constrain tenant/identity only after the existing gateway verifies the signature.
    if user['username'].casefold()!=claims['sub'].casefold() or (claims['companyId']==-1 and user.get('systemAdmin') is not True):
        raise ReportError(403, 'MES identity could not be verified')
    admin = user.get('systemAdmin') is True or user.get('admin') is True
    if user.get('systemAdmin') is not True and user.get('companyId') != company_id:
        raise ReportError(403, 'Company access denied')

    def permits(entries):
        for entry in entries:
            if entry.get('hide') or entry.get('disabled'):
                continue
            if entry.get('link') == '/inventory/inventory-activity' or permits(entry.get('children') or []):
                return True
        return False
    if not admin and not permits(info.get('menu') or []):
        raise ReportError(403, 'Inventory Activity permission required')
    # Same login, company and warehouse are required when exporting a saved snapshot.
    return hashlib.sha256(f'{header}|{company_id}|{warehouse_id}'.encode()).hexdigest()

