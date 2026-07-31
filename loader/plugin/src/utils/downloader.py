import os
import shutil
from java import dynamic_proxy
from org.telegram.messenger import NotificationCenter
from ui.bulletin import BulletinHelper
from .localization import _localize

class DownloadListener(dynamic_proxy(NotificationCenter.NotificationCenterDelegate)):
    def __init__(self, plugin, account, file_name, download_path, target_path):
        super().__init__()
        self.plugin = plugin
        self.account = account
        self.file_name = file_name
        self.download_path = download_path
        self.target_path = target_path
        self.nc = NotificationCenter.getInstance(account)

    def didReceivedNotification(self, id, account, args):
        try:
            loaded_file_name = str(args[0])
            if loaded_file_name == self.file_name:
                if id == NotificationCenter.fileLoaded:
                    self.plugin.log(f"File {self.file_name} downloaded success")
                    if os.path.exists(self.download_path):
                        shutil.copy2(self.download_path, self.target_path)
                        self.plugin.log(f"Copied to {self.target_path}")
                        BulletinHelper.show_info(_localize("installed"))
                    self.cleanup()
                elif id == NotificationCenter.fileLoadFailed:
                    self.plugin.log(f"File {self.file_name} download failed")
                    self.cleanup()
        except Exception as e:
            self.plugin.log(f"Listener error: {e}")

    def cleanup(self):
        self.nc.removeObserver(self, NotificationCenter.fileLoaded)
        self.nc.removeObserver(self, NotificationCenter.fileLoadFailed)
