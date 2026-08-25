"""DEX acquisition and loading engine.

Resolves the plugin DEX from (in order) a local sideload path, the on-disk
cache, or a fresh GitHub download, loads it into the running app, and drives
update checks for both the Release and Dev channels.
"""


class Loader:
    def __init__(self, plugin: BasePlugin, activity: Activity):
        self.plugin = plugin
        self.activity = activity
        self.instance = None
        self.dex_loader = None
        self.dex_main_class = None

        self.base_cache = os.path.join(
            ApplicationLoader.applicationContext.getFilesDir().getAbsolutePath(),
            CACHE_DIR_NAME,
        )
        self.config = Config(self.base_cache)
        self.channel = self.config.channel
        self._set_channel_paths(self.channel)

    def _set_channel_paths(self, channel):
        """Point the cache directory/file at ``channel`` and ensure it exists."""
        self.channel = channel
        self.cache_dir = os.path.join(self.base_cache, channel)
        self.cache_file = os.path.join(self.cache_dir, "cached.dat")
        os.makedirs(self.cache_dir, exist_ok=True)

    def _switch_cache(self, channel):
        self._set_channel_paths(channel)

    def getInstance(self):
        """Return the DEX ``Main`` singleton, resolving it lazily via reflection."""
        if self.instance is None:
            try:
                if self.dex_main_class is not None:
                    method = self.dex_main_class.getMethod("getInstance")
                else:
                    method = find_class(CLASS_NAME).getClass().getMethod("getInstance")
                self.instance = method.invoke(None)
            except Exception as e:
                self.plugin.log(f"Error getting instance: {e}")
        return self.instance

    # ------------------------------------------------------------------
    # DEX loading
    # ------------------------------------------------------------------

    def _load_dex_inmemory(self, bytesdex):
        buffer = ByteBuffer.wrap(bytesdex)
        loader = InMemoryDexClassLoader(
            buffer, ApplicationLoader.applicationContext.getClassLoader()
        )
        return loader.loadClass(CLASS_NAME), loader

    def _load_dex_from_file(self, dex_path):
        loader = DexClassLoader(
            dex_path,
            ApplicationLoader.applicationContext.getDir(DEX_OPT_DIR_NAME, 0).getAbsolutePath(),
            None,
            ApplicationLoader.applicationContext.getClassLoader(),
        )
        return loader.loadClass(CLASS_NAME), loader

    def _call_start(self, clazz):
        try:
            clazz.getMethod("initAndStart").invoke(None)
            self.plugin.log("Called initAndStart")
        except Exception as e:
            self.plugin.log(f"initAndStart failed: {e}")
            try:
                # Fallback for older DEX versions without initAndStart().
                instance = clazz.getMethod("getInstance").invoke(None)
                clazz.getMethod("start").invoke(instance)
                self.plugin.log("Called start() via getInstance() fallback")
            except Exception as e2:
                self.plugin.log(f"Fallback start failed: {e2}")

    def start_from_bytes(self, bytesdex):
        """Cache ``bytesdex`` to disk and load the DEX (in-memory, then file)."""
        dex_path = os.path.join(self.cache_dir, "classes.dex")

        try:
            os.makedirs(self.cache_dir, exist_ok=True)
            with open(dex_path, "wb") as f:
                f.write(bytesdex)
        except Exception as e:
            self.plugin.log(f"Failed to cache DEX to file: {e}")

        try:
            clazz, loader = self._load_dex_inmemory(bytesdex)
            self.dex_loader = loader
            self.dex_main_class = clazz
            self._call_start(clazz)
            self.plugin.log(f"Loaded {CLASS_NAME} (in-memory)")
            return
        except Exception as e:
            proxy_err = "proxy" in str(e).lower()
            self.plugin.log(f"InMemory load {'proxy issue' if proxy_err else 'failed'}: {e}")

        if os.path.exists(dex_path):
            try:
                clazz, loader = self._load_dex_from_file(dex_path)
                self.dex_loader = loader
                self.dex_main_class = clazz
                self._call_start(clazz)
                self.plugin.log(f"Loaded {CLASS_NAME} (from file)")
                return
            except Exception as e:
                self.plugin.log(f"File load also failed: {e}")

        self.plugin.log("DEX proxy unavailable — hooks from static init may still work")
        self.instance = None

    def unload(self):
        try:
            inst = self.getInstance()
            if inst:
                inst.getClass().getMethod("onUnload").invoke(inst)
                self.plugin.log("Unloaded successfully")
        except Exception as e:
            self.plugin.log(f"Error unloading: {e}")

    # ------------------------------------------------------------------
    # Version checking
    # ------------------------------------------------------------------

    def _check_dev_version(self, force=False):
        if not force and not self.config.can_check():
            self.plugin.log("Dev check skipped (rate limit cooldown)")
            self.config.data["last_error"] = "Rate limit: wait 1 min"
            return None, None
        try:
            self.config.mark_checked()
            r = requests.get(DEV_API_URL, headers={"User-Agent": USER_AGENT}, timeout=5)
            self.plugin.log(f"Dev API HTTP {r.status_code}")
            r.raise_for_status()
            runs = r.json().get("workflow_runs", [])
            if not runs:
                self.plugin.log("No successful dev runs found")
                return None, None

            run_id = runs[0].get("id", 0)
            self.plugin.log(f"Latest dev run: #{run_id}")
            return str(run_id), (DEV_RUN_URL_TEMPLATE.format(run_id), None)
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
            r = requests.get(RELEASE_API_URL, headers={"User-Agent": USER_AGENT}, timeout=5)
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
            plugin_url = None
            for asset in assets:
                name = asset.get("name", "")
                if name.endswith(".dex"):
                    dex_url = asset.get("browser_download_url", "")
                elif name.endswith("loader.plugin"):
                    plugin_url = asset.get("browser_download_url", "")
                elif name.endswith(".elyx"):
                    plugin_url = asset.get("browser_download_url", "")
                    self.plugin._is_elyx_update = True
            if not dex_url:
                self.plugin.log(f"No .dex asset found in release {tag}")
                return None, None

            self.plugin.log(f"Latest release for TG {tg_version}: {tag}")
            return tag, (dex_url, plugin_url)
        except Exception as e:
            self.plugin.log(f"Error checking release version: {e}")
            return None, None

    def _check_version(self, force=False):
        if self.channel == "dev":
            return self._check_dev_version(force)
        return self._check_release_version(force)

    def _needs_update(self, remote_version):
        cached = self.config.get_version(self.channel)
        self.plugin.log(f"Remote: {remote_version}, Cached: {cached}")
        return str(remote_version) != str(cached)

    # ------------------------------------------------------------------
    # Downloading
    # ------------------------------------------------------------------

    def _download_dev_dex(self, custom_url=None):
        url = custom_url or DEV_ARTIFACT_URL
        self.plugin.log(f"Downloading dev DEX from {url}")
        r = requests.get(url, headers=DEV_DOWNLOAD_HEADERS, timeout=60)
        r.raise_for_status()

        plugin_bytes = None
        with zipfile.ZipFile(io.BytesIO(r.content)) as zf:
            names = zf.namelist()
            dex_name = next((n for n in names if n.endswith("classes.dex")), "classes.dex")
            dex_bytes = zf.read(dex_name)

            plugin_name = next((n for n in names if n.endswith("loader.plugin")), None)
            if not plugin_name:
                plugin_name = next((n for n in names if n.endswith(".elyx")), None)
                if plugin_name:
                    self.plugin._is_elyx_update = True
            if plugin_name:
                plugin_bytes = zf.read(plugin_name)

        self.plugin.log(f"Downloaded {len(dex_bytes)} bytes from nightly.link")
        return dex_bytes, plugin_bytes

    def _download_release_dex(self, url):
        self.plugin.log(f"Downloading release DEX from {url}")
        r = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=60)
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
                    r = requests.get(plugin_url, headers={"User-Agent": USER_AGENT}, timeout=60)
                    r.raise_for_status()
                    plugin_bytes = r.content
                except Exception as e:
                    self.plugin.log(f"Failed to download release plugin: {e}")

        with open(self.cache_file, "wb") as f:
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
        try:
            if getattr(self.plugin, "_is_elyx_update", False):
                dl_path = os.path.join("/sdcard/Download", "re_extera_loader.elyx")
                with open(dl_path, "wb") as f:
                    f.write(plugin_bytes)
                self.plugin.log(f"Elyx update saved to {dl_path}")
                AndroidUtilities.runOnUIThread(UIRunnable(lambda: BulletinHelper.show_info(
                    "re:extera .elyx update downloaded to Downloads directory! Please install from file.",
                    get_last_fragment(),
                )))
                return

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

    # ------------------------------------------------------------------
    # DEX source resolution
    # ------------------------------------------------------------------

    def _load_from_local_path(self):
        if os.path.exists(LOCAL_DEX_PATH):
            self.plugin.log(f"Local DEX found at {LOCAL_DEX_PATH}")
            with open(LOCAL_DEX_PATH, "rb") as f:
                return f.read()
        return None

    def _load_from_cache(self):
        if os.path.exists(self.cache_file):
            with open(self.cache_file, "rb") as f:
                return f.read()
        return None

    def load_and_start(self):
        """Resolve and load the DEX from local path, cache, or a fresh download."""
        self.plugin.log(f"Loading (channel: {self.channel})")

        local_bytes = self._load_from_local_path()
        if local_bytes is not None:
            try:
                self.start_from_bytes(local_bytes)
                self.plugin.log("Loaded from local storage")
                self._safe_async_update()
                return
            except Exception as e:
                self.plugin.log(f"Local DEX failed ({e}), falling through to cache/download")
                try:
                    os.remove(LOCAL_DEX_PATH)
                    self.plugin.log("Removed stale local DEX")
                except Exception:
                    pass

        cached_bytes = self._load_from_cache()
        if cached_bytes is not None:
            try:
                self.start_from_bytes(cached_bytes)
                self.plugin.log("Loaded from cache")
                self._safe_async_update()
            except Exception as e:
                self.plugin.log(f"Cached DEX failed ({e}), will download fresh")
        else:
            self.plugin.log("No cache found")

        if self.instance is None:
            self.plugin.log("Downloading fresh DEX...")
            try:
                remote_version, download_url = self._check_version()
                if remote_version is None:
                    return
                dex_bytes = self.download_and_cache(remote_version, download_url)
                self.start_from_bytes(dex_bytes)
                self.plugin.log("Loaded from fresh download")
            except Exception as e:
                if "proxy" in str(e):
                    return
                self.plugin.log(f"Fatal error: {e}")

    def _safe_async_update(self):
        try:
            self._check_async_update()
        except Exception as e:
            self.plugin.log(f"Update check failed: {e}")

    # ------------------------------------------------------------------
    # Update prompts
    # ------------------------------------------------------------------

    def _check_async_update(self):
        def run_check():
            try:
                remote_version, download_url = self._check_version()
                if remote_version == "UNSUPPORTED_VERSION":
                    AndroidUtilities.runOnUIThread(UIRunnable(
                        lambda: BulletinHelper.show_info(_localize("no_official_update"), get_last_fragment())
                    ))
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
                    if args and hasattr(args[0], "dismiss"):
                        args[0].dismiss()
                except Exception:
                    pass
                BulletinHelper.show_info(_localize("downloading"), get_last_fragment())

                def run_download():
                    try:
                        self.download_and_cache(remote_version, download_url)
                        AndroidUtilities.runOnUIThread(UIRunnable(
                            lambda: BulletinHelper.show_info(_localize("update_avail"), get_last_fragment())
                        ))
                    except Exception as e:
                        msg = f"Update failed: {e}"
                        AndroidUtilities.runOnUIThread(UIRunnable(
                            lambda: BulletinHelper.show_info(msg, get_last_fragment())
                        ))

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
                    head_run = requests.get(
                        f"https://api.github.com/repos/fossSquad/re-extera/actions/runs/{head}",
                        headers={"User-Agent": USER_AGENT},
                    ).json()
                    base_run = requests.get(
                        f"https://api.github.com/repos/fossSquad/re-extera/actions/runs/{base}",
                        headers={"User-Agent": USER_AGENT},
                    ).json()
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

                show_dialog("\n".join(log_lines) if log_lines else "No commits found in compare.")
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
                        AndroidUtilities.runOnUIThread(UIRunnable(
                            lambda: BulletinHelper.show_info(_localize("no_official_update"), get_last_fragment())
                        ))
                        return
                    if remote_version is None:
                        err = self.config.data.get("last_error", "")
                        msg = f"Check failed: {err}" if err else "Failed to check updates"
                        AndroidUtilities.runOnUIThread(UIRunnable(
                            lambda: BulletinHelper.show_info(msg, get_last_fragment())
                        ))
                        return

                    if self._needs_update(remote_version):
                        self.download_and_cache(remote_version, download_url)
                        AndroidUtilities.runOnUIThread(UIRunnable(
                            lambda: BulletinHelper.show_info(_localize("update_avail"), get_last_fragment())
                        ))
                    else:
                        AndroidUtilities.runOnUIThread(UIRunnable(
                            lambda: BulletinHelper.show_info(_localize("up_to_date"), get_last_fragment())
                        ))
                except Exception as e:
                    self.plugin.log(f"Manual update check failed: {e}")
                    msg = f"Error: {e}"
                    AndroidUtilities.runOnUIThread(UIRunnable(
                        lambda: BulletinHelper.show_info(msg, get_last_fragment())
                    ))

            threading.Thread(target=run_manual).start()
        except Exception as e:
            self.plugin.log(f"Manual check start failed: {e}")
            BulletinHelper.show_info(f"Error: {e}", get_last_fragment())

    def get_version_display(self):
        try:
            if self.dex_main_class is not None:
                v = self.dex_main_class.getDeclaredField(VERSION_FIELD_NAME)
                v.setAccessible(True)
                return f"v{v.get(None)}"
            return "not loaded"
        except Exception:
            cached = self.config.get_version(self.channel)
            return f"cached:{cached}" if cached else "?"
