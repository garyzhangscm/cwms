#!/usr/bin/env python3
"""Overlay the standalone card on an isolated Web checkout without altering its existing form."""
from pathlib import Path
import shutil, sys
root=Path(sys.argv[1]); bundle=Path(__file__).resolve().parents[1]/'web'
module=root/'src/app/routes/outbound/outbound.module.ts'
text=module.read_text(); import_line="import { InTransitShippingComponent } from './in-transit-shipping/in-transit-shipping.component';\n"
if import_line not in text:
    assert text.count('imports: [')==1
    text=import_line+text.replace('imports: [','imports: [\n        InTransitShippingComponent,',1)
    module.write_text(text)
template=root/'src/app/routes/outbound/outbound-configuration/outbound-configuration.component.html'
text=template.read_text();card='\n<app-in-transit-shipping-configuration [displayOnly]="displayOnly"></app-in-transit-shipping-configuration>\n'
if 'app-in-transit-shipping-configuration' not in text: template.write_text(text+card)
for source in bundle.rglob('*.ts'):
    dest=root/source.relative_to(bundle);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest)
print('In-transit card applied; existing outbound form retained')
