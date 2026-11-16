"""Completed inventory activities, not scanner keystrokes. All event timestamps are UTC."""
from collections import defaultdict
from datetime import timezone
import hashlib

KINDS=('receiving','placement','movement','picking','excluded')
SYSTEM={'anonymous','cwms_inner_call','cwms-external-handoff','cwms-user'}

def actor(username):
    name=(username or '').strip()
    group='unknown' if not name or name.casefold()=='anonymous' else 'system' if name.casefold() in SYSTEM else 'shared' if name.casefold()=='shipping' else 'user'
    return name or '(not recorded)',group

def location_map(rows):
    out=defaultdict(list)
    for r in rows:out[r['name']].append(r)
    return out

def classify(row,locations):
    if row['type']=='RECEIVING':return 'receiving',''
    if row['type']=='PICKING':return 'picking',''
    src,dst=locations.get(row['from_value'],[]),locations.get(row['to_value'],[])
    if len(src)!=1 or len(dst)!=1:return 'excluded','Location missing or ambiguous'
    s,d=src[0],dst[0]
    if s['isVirtual'] or d['isVirtual']:return 'excluded','Virtual/system inventory transition'
    if s['name']==d['name']:return 'excluded','Same location'
    if d['isRF']:return 'excluded','Pickup onto RF; terminal deposit is counted separately'
    if d['isStorage'] and not s['isStorage']:return 'placement','Into storage; business page is not recorded'
    return 'movement','Physical transfer; excludes RF pickup legs and virtual transitions'

def aggregate(rows,locations):
    people={};events=[];seen=set()
    for row in rows:
        # Each persisted activity is one event. A repeated LPN at another time is valid work.
        identity=row['id']
        if identity in seen:continue
        seen.add(identity)
        username,group=actor(row['username']);kind,note=classify(row,locations)
        key=hashlib.sha256(username.encode()).hexdigest()[:24]
        person=people.setdefault(key,dict(key=key,username=username,group=group,**{k:0 for k in KINDS}))
        person[kind]+=1
        stamp=row['timestamp']
        if stamp.tzinfo is not None:raise ValueError('Expected UTC DATETIME without timezone')
        events.append(dict(id=str(identity),userKey=key,username=username,group=group,kind=kind,
            time=stamp.replace(tzinfo=timezone.utc).isoformat(),lpn=row.get('lpn') or '',
            product=row.get('product') or '',quantity=str(row.get('quantity') or 0),
            source=row.get('from_value') or '' if row['type']=='INVENTORY_MOVEMENT' else '',
            destination=row.get('to_value') or '' if row['type']=='INVENTORY_MOVEMENT' else row.get('location') or '',
            document=row.get('document') or '',rf=row.get('rf') or '',note=note))
    events.sort(key=lambda e:(e['time'],int(e['id'])),reverse=True)
    return sorted(people.values(),key=lambda p:(p['group'],p['username'].casefold())),events
