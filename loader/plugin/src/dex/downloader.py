import os
import requests
import zipfile
import io

from ..constants import (
    USER_AGENT, DEV_DOWNLOAD_HEADERS, DEV_ARTIFACT_URL, LOCAL_DEX_PATH
)

class DownloadMixin:
    def _download_dev_dex(self, custom_url=None):
        url = custom_url if custom_url else DEV_ARTIFACT_URL
        self.plugin.log(f"Downloading dev DEX from {url}")
        r = requests.get(url, headers=DEV_DOWNLOAD_HEADERS, timeout=60)
        r.raise_for_status()
        with zipfile.ZipFile(io.BytesIO(r.content)) as zf:
            dex_path = next((n for n in zf.namelist() if n.endswith("classes.dex")), "classes.dex")
            dex_bytes = zf.read(dex_path)
            try:
                plugin_path = next((n for n in zf.namelist() if n.endswith("loader.elyx")), None)
                if not plugin_path:
                    plugin_path = next((n for n in zf.namelist() if n.endswith("loader.plugin")), "loader.plugin")
                plugin_bytes = zf.read(plugin_path)
            except Exception:
                plugin_bytes = None
        self.plugin.log(f"Downloaded {len(dex_bytes)} bytes from nightly.link")
        return dex_bytes, plugin_bytes

    def _download_release_dex(self, url):
        self.plugin.log(f"Downloading release DEX from {url}")
        headers = {"User-Agent": USER_AGENT}
        r = requests.get(url, headers=headers, timeout=60)
        r.raise_for_status()
        self.plugin.log(f"Downloaded {len(r.content)} bytes from releases")
        return r.content

    def download_and_cache(self, remote_version, urls):
        dex_url, plugin_url = urls
        plugin_bytes = None

        if self.channel == "dev":
            dex_bytes, plugin_bytes = self._download_dev_dex(dex_url)
        else:
            dex_bytes = self._download_release_dex(dex_url)
            if plugin_url:
                try:
                    headers = {"User-Agent": USER_AGENT}
                    r = requests.get(plugin_url, headers=headers, timeout=60)
                    r.raise_for_status()
                    plugin_bytes = r.content
                except Exception as e:
                    self.plugin.log(f"Failed to download release plugin: {e}")

        with open(self.cache_file, 'wb') as f:
            f.write(dex_bytes)
            
        if os.path.exists(LOCAL_DEX_PATH):
            try:
                os.remove(LOCAL_DEX_PATH)
                self.plugin.log("Removed LOCAL_DEX_PATH because a new version was explicitly downloaded")
            except Exception as e:
                self.plugin.log(f"Failed to remove LOCAL_DEX_PATH: {e}")

        if plugin_bytes:
            self._update_plugin_file(plugin_bytes)

        self.config.set_version(self.channel, remote_version)
        return dex_bytes

    def _update_plugin_file(self, plugin_bytes):
        self.plugin.log("Auto-updating loader is not supported in Elyx yet")
        return

    def __unused(self):
        try:
            plugin_path = __file__
            if plugin_path.endswith(".pyc"):
                plugin_path = plugin_path[:-1]
                
            if plugin_path and os.path.exists(plugin_path):
                with open(plugin_path, "rb") as f:
                    old_bytes = f.read()
                if old_bytes != plugin_bytes:
                    with open(plugin_path, "wb") as f:
                        f.write(plugin_bytes)
                    self.plugin.log("Plugin updated successfully (requires restart)")
        except Exception as e:
            self.plugin.log(f"Failed to update plugin: {e}")
