"""Authenticated operator API for Oracle Item import exceptions."""
import argparse
import csv
import io
import json
import threading
from pathlib import Path
import adapter
from oracle_items import HEADER_ALIASES
from work_order_api import WorkOrderStore, Handler, Server

class ItemStore(WorkOrderStore):
    subject = 'Item'
    section = 'oracleItems'
    kind = 'items'
    number_key = 'itemName'
    raw_number_key = 'item_number'
    role = 'Item Import Operator'
    route = '/item-import'
    target_key = 'target_item'
    flag = 'itemExceptionHandling'
    guard_class = adapter.ExistingItemGuard

    def raw_rows(self, content):
        reader = csv.DictReader(io.StringIO(content.decode('utf-8-sig')))
        reader.fieldnames = [HEADER_ALIASES.get(k.strip().lower().replace(' ', '_'),
                            k.strip().lower().replace(' ', '_')) for k in reader.fieldnames or []]
        return list(reader)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    args = parser.parse_args()
    store = ItemStore(json.loads(args.config.read_text()))
    server = Server((store.config['operatorApi']['bind'], store.config['operatorApi']['port']), Handler)
    server.store = store
    threading.Thread(target=store.worker, daemon=True).start()
    server.serve_forever()

if __name__ == '__main__':
    main()
