import os
from ui.bulletin import BulletinHelper
from client_utils import get_last_fragment
from org.telegram.messenger import AndroidUtilities

from ..utils import _localize
from ..constants import LOCAL_DEX_PATH

class ActionsMixin:
    def _on_channel_switch(self, is_dev):
        if self.loader is None:
            return
        new_channel = "dev" if is_dev else "release"
        old_channel = self.loader.config.channel
        if new_channel == old_channel:
            return

        self.loader.config.channel = new_channel
        self.loader._switch_cache(new_channel)
        self.log(f"Switched to {new_channel} channel")
        BulletinHelper.show_info(_localize("channel_switch"), get_last_fragment())

    def _on_check_updates(self):
        if self.loader is None:
            return
        self.loader.check_updates_now()

    def _on_install_file(self):
        if self.loader is None:
            return
        if os.path.exists(LOCAL_DEX_PATH):
            try:
                with open(LOCAL_DEX_PATH, 'rb') as f:
                    dex_bytes = f.read()
                self.loader.start_from_bytes(dex_bytes)
                BulletinHelper.show_info(_localize("updated_cache"), get_last_fragment())
                self.log("Reloaded from local file")
            except Exception as e:
                self.log(f"Install from file failed: {e}")
                BulletinHelper.show_info(f"Error: {e}", get_last_fragment())
        else:
            BulletinHelper.show_info(_localize("file_not_found"), get_last_fragment())

    def _on_copy_logs(self):
        try:
            java_logs = ""
            if self.loader and self.loader.dex_main_class:
                try:
                    get_logs_method = self.loader.dex_main_class.getMethod("getLogs")
                    java_logs = str(get_logs_method.invoke(None))
                except Exception as e:
                    self.log(f"Failed to fetch Java logs: {e}")
                    
            plugin_logs = "\n".join(self._logs) if self._logs else "No plugin logs"
            
            full_logs = "=== Loader Logs ===\n" + plugin_logs
            if java_logs:
                full_logs += "\n\n=== Hook Logs ===\n" + java_logs
                
            AndroidUtilities.addToClipboard(full_logs)
            BulletinHelper.show_info(_localize("logs_copied"), get_last_fragment())
        except Exception as e:
            self.log(f"Error copying logs: {e}")
            BulletinHelper.show_info(f"Error: {e}", get_last_fragment())

    def _open_re_extera_settings(self):
        try:
            if self.loader is None or self.loader.dex_main_class is None:
                self.log("DEX not loaded")
                return
            try:
                show_method = self.loader.dex_main_class.getMethod("showSettingsExternal")
                show_method.invoke(None)
            except Exception as e:
                self.log(f"showSettingsExternal failed, trying fallback: {e}")
                get_instance = self.loader.dex_main_class.getMethod("getInstance")
                instance = get_instance.invoke(None)
                show_method = self.loader.dex_main_class.getMethod("showSettings")
                show_method.invoke(instance)
        except Exception as e:
            self.log(f"Error opening DEX settings: {e}")
