import os
from org.telegram.messenger import ApplicationLoader
from base_plugin import BasePlugin
from android.app import Activity

from ..config import Config
from ..constants import CACHE_DIR_NAME

from .core import CoreMixin
from .updater import UpdateMixin
from .downloader import DownloadMixin
from .ui import UIMixin

class Loader(CoreMixin, UpdateMixin, DownloadMixin, UIMixin):
    def __init__(self, plugin: BasePlugin, activity: Activity):
        self.plugin = plugin
        self.activity = activity
        self.instance = None
        self.dex_loader = None
        self.dex_main_class = None

        base_cache = os.path.join(
            ApplicationLoader.applicationContext.getFilesDir().getAbsolutePath(),
            CACHE_DIR_NAME
        )
        self.config = Config(base_cache)
        self.channel = self.config.channel
        self.cache_dir = os.path.join(base_cache, self.channel)
        self.cache_file = os.path.join(self.cache_dir, "cached.dat")

        if not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)
