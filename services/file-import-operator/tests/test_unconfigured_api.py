import json
import threading
import unittest
import urllib.request
import urllib.error
from http.server import ThreadingHTTPServer
from bootstrap import disabled_handler

class DisabledSource(unittest.TestCase):
    def test_health_is_explicit_and_all_business_methods_are_rejected(self):
        server=ThreadingHTTPServer(('127.0.0.1',0),disabled_handler('Work Order import source is not configured for this factory.'))
        thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        root='http://127.0.0.1:'+str(server.server_port)
        try:
            with urllib.request.urlopen(root+'/healthz') as response:
                self.assertFalse(json.load(response)['data']['configured'])
            for method in ['GET','POST','PUT','DELETE']:
                request=urllib.request.Request(root+'/work-order-import/actions',method=method)
                with self.assertRaises(urllib.error.HTTPError) as caught:urllib.request.urlopen(request)
                self.assertEqual(caught.exception.code,503)
                self.assertIn('not configured',json.load(caught.exception)['message'])
        finally:
            server.shutdown();server.server_close();thread.join()
