"""Image entrypoint; runtime credentials/configuration are supplied by read-only mounts."""
import argparse
import json
import threading
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import adapter


def disabled_handler(reason):
    class Handler(BaseHTTPRequestHandler):
        def respond(self):
            healthy = self.command == 'GET' and self.path == '/healthz'
            body = json.dumps({'result': 0 if healthy else 1,
                               'message': '' if healthy else reason,
                               'data': {'configured': False} if healthy else None}).encode()
            self.send_response(200 if healthy else 503)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        do_GET = respond
        do_POST = respond
        do_PUT = respond
        do_DELETE = respond
        def log_message(self, *args):
            pass
    return Handler


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--kind', choices=['items', 'work-orders'], required=True)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--unconfigured-reason')
    args = parser.parse_args()
    config = json.loads(args.config.read_text())
    host, port = config['operatorApi']['bind'], config['operatorApi']['port']
    if args.unconfigured_reason:
        ThreadingHTTPServer((host, port), disabled_handler(args.unconfigured_reason)).serve_forever()
        return
    if args.kind == 'items':
        adapter.oracle_item_file_pattern(config)
        from item_import_api import ItemStore as Store
    else:
        from work_order_api import WorkOrderStore as Store
    if type(config.get('deleteSourceFiles', True)) is not bool:
        raise ValueError('deleteSourceFiles must be true or false')
    from work_order_api import Handler, Server
    store = Store(config)
    server = Server((host, port), Handler)
    server.store = store
    threading.Thread(target=store.worker, daemon=True).start()
    server.serve_forever()


if __name__ == '__main__':
    main()
