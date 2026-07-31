import threading
import requests
from org.telegram.messenger import AndroidUtilities
from ui.bulletin import BulletinHelper
from ui.alert import AlertDialogBuilder
from client_utils import get_last_fragment

from ..utils import UIRunnable, _localize
from ..constants import USER_AGENT

class UIMixin:
    def _check_async_update(self):
        def run_check():
            try:
                remote_version, download_url = self._check_version()
                if remote_version == "UNSUPPORTED_VERSION":
                    def show_warn():
                        BulletinHelper.show_info(_localize("no_official_update"), get_last_fragment())
                    AndroidUtilities.runOnUIThread(UIRunnable(show_warn))
                    return
                if remote_version is None:
                    return

                if self._needs_update(remote_version):
                    self.plugin.log("Update available, prompting user...")
                    self._prompt_update(remote_version, download_url)
            except Exception as e:
                self.plugin.log(f"Async update failed: {e}")

        threading.Thread(target=run_check).start()

    def _prompt_update(self, remote_version, download_url):
        cached = self.config.get_version(self.channel)
        
        def show_dialog(changelog_text):
            def do_update(*args):
                try:
                    if args and hasattr(args[0], 'dismiss'):
                        args[0].dismiss()
                except Exception:
                    pass
                BulletinHelper.show_info(_localize("downloading"), get_last_fragment())
                def run_download():
                    try:
                        self.download_and_cache(remote_version, download_url)
                        AndroidUtilities.runOnUIThread(UIRunnable(lambda: BulletinHelper.show_info(_localize("update_avail"), get_last_fragment())))
                    except Exception as e:
                        AndroidUtilities.runOnUIThread(UIRunnable(lambda: BulletinHelper.show_info(f"Update failed: {e}", get_last_fragment())))
                threading.Thread(target=run_download).start()

            def on_ui():
                context = get_last_fragment().getParentActivity()
                bld = AlertDialogBuilder(context)
                bld.set_title(f"re:extera has updated to version {remote_version}")
                bld.set_message(changelog_text)
                bld.set_positive_button("Update", do_update)
                bld.set_negative_button("Later", lambda *args: args[0].dismiss() if args else None)
                bld.show()
                
            AndroidUtilities.runOnUIThread(UIRunnable(on_ui))

        def fetch_changelog():
            try:
                if not cached or cached == "0":
                    show_dialog("No previous version found to compare.")
                    return
                
                base = str(cached)
                head = str(remote_version)
                
                if self.channel == "dev":
                    head_run = requests.get(f"https://api.github.com/repos/fossSquad/re-extera/actions/runs/{head}", headers={"User-Agent": USER_AGENT}).json()
                    base_run = requests.get(f"https://api.github.com/repos/fossSquad/re-extera/actions/runs/{base}", headers={"User-Agent": USER_AGENT}).json()
                    head = head_run.get("head_sha", head)
                    base = base_run.get("head_sha", base)
                
                if base == head:
                    show_dialog("No changes.")
                    return
                    
                compare_url = f"https://api.github.com/repos/fossSquad/re-extera/compare/{base}...{head}"
                r = requests.get(compare_url, headers={"User-Agent": USER_AGENT})
                r.raise_for_status()
                commits = r.json().get("commits", [])
                
                log_lines = []
                for c in commits:
                    msg = c["commit"]["message"].split("\n")[0]
                    author = c["author"]["login"] if c.get("author") else c["commit"]["author"]["name"]
                    log_lines.append(f"- {msg} ({author})")
                
                if not log_lines:
                    changelog_text = "No commits found in compare."
                else:
                    changelog_text = "\n".join(log_lines)
                show_dialog(changelog_text)
            except Exception as e:
                self.plugin.log(f"Failed to fetch changelog: {e}")
                show_dialog("Changelog unavailable.")
                
        threading.Thread(target=fetch_changelog).start()

    def check_updates_now(self):
        try:
            BulletinHelper.show_info(_localize("downloading"), get_last_fragment())
            def run_manual():
                try:
                    remote_version, download_url = self._check_version(force=True)
                    if remote_version == "UNSUPPORTED_VERSION":
                        def show_warn():
                            BulletinHelper.show_info(_localize("no_official_update"), get_last_fragment())
                        AndroidUtilities.runOnUIThread(UIRunnable(show_warn))
                        return
                    if remote_version is None:
                        err = self.config.data.get("last_error", "")
                        msg = f"Check failed: {err}" if err else "Failed to check updates"
                        def show_err():
                            BulletinHelper.show_info(msg, get_last_fragment())
                        AndroidUtilities.runOnUIThread(UIRunnable(show_err))
                        return

                    if self._needs_update(remote_version):
                        self.download_and_cache(remote_version, download_url)
                        def show_success():
                            BulletinHelper.show_info(_localize("update_avail"), get_last_fragment())
                        AndroidUtilities.runOnUIThread(UIRunnable(show_success))
                    else:
                        def show_uptodate():
                            BulletinHelper.show_info(_localize("up_to_date"), get_last_fragment())
                        AndroidUtilities.runOnUIThread(UIRunnable(show_uptodate))

                except Exception as e:
                    self.plugin.log(f"Manual update check failed: {e}")
                    err_str = str(e)
                    def show_exc():
                        BulletinHelper.show_info(f"Error: {err_str}", get_last_fragment())
                    AndroidUtilities.runOnUIThread(UIRunnable(show_exc))

            threading.Thread(target=run_manual).start()

        except Exception as e:
            self.plugin.log(f"Manual check start failed: {e}")
            BulletinHelper.show_info(f"Error: {e}", get_last_fragment())
