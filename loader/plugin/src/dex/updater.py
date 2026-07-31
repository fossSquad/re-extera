import requests
from org.telegram.messenger import BuildVars

from ..constants import (
    DEV_API_URL, DEV_RUN_URL_TEMPLATE, RELEASE_API_URL, USER_AGENT
)

class UpdateMixin:
    def _get_cached_version(self):
        try:
            inst = self.getInstance()
            if inst is None:
                return 0
            version_field = inst.getClass().getDeclaredField("VERSION_CODE")
            version_field.setAccessible(True)
            return int(version_field.get(None))
        except Exception as e:
            self.plugin.log(f"Error reading cached version: {e}")
            return 0

    def _check_dev_version(self, force=False):
        if not force and not self.config.can_check():
            self.plugin.log("Dev check skipped (rate limit cooldown)")
            self.config.last_error = "Rate limit: wait 1 min"
            return None, None
        try:
            self.config.mark_checked()
            headers = {"User-Agent": USER_AGENT}
            r = requests.get(DEV_API_URL, headers=headers, timeout=5)
            self.plugin.log(f"Dev API HTTP {r.status_code}")
            r.raise_for_status()
            data = r.json()
            runs = data.get("workflow_runs", [])
            if not runs:
                self.plugin.log("No successful dev runs found")
                return None, None

            run = runs[0]
            run_id = run.get("id", 0)
            self.plugin.log(f"Latest dev run: #{run_id}")
            dev_url = DEV_RUN_URL_TEMPLATE.format(run_id)
            return str(run_id), (dev_url, None)
        except Exception as e:
            self.plugin.log(f"Error checking dev version: {e}")
            return None, None

    def _check_release_version(self, force=False):
        if not force and not self.config.can_check():
            self.plugin.log("Release check skipped (rate limit cooldown)")
            self.config.data["last_error"] = "Rate limit: wait 1 min"
            return None, None
        try:
            self.config.mark_checked()
            headers = {"User-Agent": USER_AGENT}
            r = requests.get(RELEASE_API_URL, headers=headers, timeout=5)
            self.plugin.log(f"Release API HTTP {r.status_code}")
            r.raise_for_status()
            releases = r.json()
            
            tg_version = BuildVars.BUILD_VERSION_STRING
            suffix = f"-{tg_version}"
            
            target_release = None
            for release in releases:
                if release.get("tag_name", "").endswith(suffix):
                    target_release = release
                    break
            
            if not target_release:
                self.plugin.log(f"No release found for Telegram version {tg_version}")
                return "UNSUPPORTED_VERSION", None

            tag = target_release.get("tag_name", "")
            assets = target_release.get("assets", [])
            
            if not tag or not assets:
                self.plugin.log("No release assets found in target release")
                return None, None

            dex_url = None
            elyx_url = None
            plugin_fallback_url = None
            for asset in assets:
                name = asset.get("name", "")
                if name.endswith(".dex"):
                    dex_url = asset.get("browser_download_url", "")
                elif name.endswith("loader.elyx"):
                    elyx_url = asset.get("browser_download_url", "")
                elif name.endswith("loader.plugin"):
                    plugin_fallback_url = asset.get("browser_download_url", "")
            
            if not dex_url:
                self.plugin.log(f"No .dex asset found in release {tag}")
                return None, None

            final_plugin_url = elyx_url if elyx_url else plugin_fallback_url
            self.plugin.log(f"Latest release for TG {tg_version}: {tag}")
            return tag, (dex_url, final_plugin_url)
        except Exception as e:
            self.plugin.log(f"Error checking release version: {e}")
            return None, None

    def _needs_update(self, remote_version):
        cached = self.config.get_version(self.channel)
        self.plugin.log(f"Remote: {remote_version}, Cached: {cached}")
        return str(remote_version) != str(cached)

    def _check_version(self, force=False):
        if self.channel == "dev":
            return self._check_dev_version(force)
        return self._check_release_version(force)
