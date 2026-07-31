import time
import threading
from elyx import metainfo
from ui.bulletin import BulletinHelper
from client_utils import get_last_fragment

from .dex import Loader

class CorePluginMixin:
    def __init__(self):
        super().__init__()
        self.loader = None
        self._logs = []
        self._api_cache = {}

    def _fetch_cached(self, key, fetch_func, callback, ttl=120):
        now = time.time()
        if key in self._api_cache:
            ts, data = self._api_cache[key]
            if now - ts < ttl:
                callback(data)
                return
                
        def wrapper():
            data = fetch_func()
            if data is not None:
                self._api_cache[key] = (time.time(), data)
            callback(data)
            
        threading.Thread(target=wrapper).start()

    def log(self, message):
        import datetime
        ts = datetime.datetime.now().strftime("%H:%M:%S")
        self._logs.append(f"[{ts}] {message}")
        try:
            super().log(message)
        except AttributeError:
            pass

    def on_plugin_load(self) -> None:
        try:
            self.log(f"Init {metainfo['version']}")
            launch_activity = get_last_fragment().getContext()
            self.loader = Loader(self, launch_activity)
            self.loader.load_and_start()
        except Exception as e:
            BulletinHelper.show_info(f"Error: {e}", get_last_fragment())
            self.log(f"Error: {e}")

    def on_plugin_unload(self) -> None:
        if self.loader is not None:
            self.loader.unload()
