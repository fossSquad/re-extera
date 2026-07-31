import threading
import requests
from ui.alert import AlertDialogBuilder
from ui.bulletin import BulletinHelper
from client_utils import get_last_fragment
from org.telegram.messenger import AndroidUtilities, BuildVars

from ..utils import UIRunnable, _localize
from ..constants import DEV_RUN_URL_TEMPLATE, RELEASE_API_URL, USER_AGENT

class VersionSelectorMixin:
    def _show_list_dialog(self, title, items, on_click):
        context = get_last_fragment().getParentActivity()
        bld = AlertDialogBuilder(context)
        bld.set_title(title)
        bld.set_items(items, on_click)
        bld.set_negative_button("Cancel", lambda *args: args[0].dismiss() if args else None)
        bld.show()

    def _download_and_apply_version(self, version, urls):
        BulletinHelper.show_info(_localize("downloading"), get_last_fragment())
        def run_dl():
            try:
                self.loader.download_and_cache(version, urls)
                AndroidUtilities.runOnUIThread(UIRunnable(lambda: BulletinHelper.show_info(_localize("update_avail"), get_last_fragment())))
            except Exception as e:
                self.log(f"Failed to download version {version}: {e}")
                AndroidUtilities.runOnUIThread(UIRunnable(lambda: BulletinHelper.show_info(f"Error: {e}", get_last_fragment())))
        threading.Thread(target=run_dl).start()

    def _on_select_version(self):
        if self.loader is None:
            return
        
        channel = self.loader.config.channel
        if channel == "dev":
            self._select_dev_branch()
        else:
            self._select_release_tg_version()

    def _select_dev_branch(self):
        def on_loaded(branches):
            if not branches:
                BulletinHelper.show_info("No branches found", get_last_fragment())
                return
            
            def on_click(bld, idx):
                bld.dismiss()
                self._select_dev_run(branches[idx])
                
            AndroidUtilities.runOnUIThread(UIRunnable(lambda: self._show_list_dialog("Select Branch", branches, on_click)))
            
        def fetch():
            try:
                r = requests.get("https://api.github.com/repos/fossSquad/re-extera/branches", headers={"User-Agent": USER_AGENT})
                r.raise_for_status()
                return [b["name"] for b in r.json()]
            except Exception as e:
                self.log(f"Failed to fetch branches: {e}")
                return None
                
        self._fetch_cached("dev_branches", fetch, lambda d: on_loaded(d if d else []))

    def _select_dev_run(self, branch):
        def on_loaded(runs):
            if not runs:
                BulletinHelper.show_info("No runs found", get_last_fragment())
                return
            
            run_labels = [f"#{r['id']} - {r['head_commit']['message'][:20]}" for r in runs]
            if run_labels:
                run_labels[0] = "⭐ " + run_labels[0]
            
            def on_click(bld, idx):
                bld.dismiss()
                run_id = runs[idx]["id"]
                dev_url = DEV_RUN_URL_TEMPLATE.format(run_id)
                self._download_and_apply_version(str(run_id), (dev_url, None))
                
            AndroidUtilities.runOnUIThread(UIRunnable(lambda: self._show_list_dialog("Select Build", run_labels, on_click)))
            
        def fetch():
            try:
                r = requests.get(f"https://api.github.com/repos/fossSquad/re-extera/actions/workflows/build.yml/runs?branch={branch}&status=success&per_page=10", headers={"User-Agent": USER_AGENT})
                r.raise_for_status()
                return r.json().get("workflow_runs", [])
            except Exception as e:
                self.log(f"Failed to fetch runs: {e}")
                return None
                
        self._fetch_cached(f"dev_runs_{branch}", fetch, lambda d: on_loaded(d if d else []))

    def _select_release_tg_version(self):
        def on_loaded(releases_by_tg, current_tg):
            if not releases_by_tg:
                BulletinHelper.show_info("No releases found", get_last_fragment())
                return
            
            tg_versions = list(releases_by_tg.keys())
            if current_tg in tg_versions:
                tg_versions.remove(current_tg)
                tg_versions.insert(0, current_tg)
            
            labels = [f"⭐ {v}" if v == current_tg else v for v in tg_versions]
            
            def on_click(bld, idx):
                bld.dismiss()
                self._select_release_build(releases_by_tg[tg_versions[idx]], tg_versions[idx] == current_tg)
                
            AndroidUtilities.runOnUIThread(UIRunnable(lambda: self._show_list_dialog("Select Telegram Version", labels, on_click)))
            
        def fetch():
            try:
                r = requests.get(RELEASE_API_URL, headers={"User-Agent": USER_AGENT})
                r.raise_for_status()
                releases = r.json()
                
                grouped = {}
                for rel in releases:
                    tag = rel.get("tag_name", "")
                    if "-" in tag:
                        tg_ver = tag.split("-")[-1]
                        if tg_ver not in grouped:
                            grouped[tg_ver] = []
                        grouped[tg_ver].append(rel)
                return grouped
            except Exception as e:
                self.log(f"Failed to fetch releases: {e}")
                return None
                
        current_tg = BuildVars.BUILD_VERSION_STRING
        self._fetch_cached("releases", fetch, lambda d: on_loaded(d if d else {}, current_tg))

    def _select_release_build(self, releases, is_compatible=False):
        labels = []
        for rel in releases:
            tag = rel.get("tag_name", "")
            if "-" in tag:
                labels.append(tag.split("-")[0])
            else:
                labels.append(tag)
                
        if labels and is_compatible:
            labels[0] = "⭐ " + labels[0]
        
        def on_click(bld, idx):
            bld.dismiss()
            rel = releases[idx]
            tag = rel.get("tag_name", "")
            assets = rel.get("assets", [])
            dex_url = None
            plugin_url = None
            for asset in assets:
                name = asset.get("name", "")
                if name.endswith(".dex"):
                    dex_url = asset.get("browser_download_url", "")
                elif name.endswith("loader.plugin") or name.endswith("loader.elyx"):
                    plugin_url = asset.get("browser_download_url", "")
                    
            if dex_url:
                self._download_and_apply_version(tag, (dex_url, plugin_url))
            else:
                BulletinHelper.show_info("No DEX in this release", get_last_fragment())
                
        self._show_list_dialog("Select Release", labels, on_click)
