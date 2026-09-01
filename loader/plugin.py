class Plugin(BasePlugin):
    def __init__(self):
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

    def create_settings(self) -> List[Any]:
        items = []

        try:
            channel = self.loader.config.channel if self.loader else "release"
            version = self.loader.get_version_display() if self.loader else "?"
        except Exception:
            channel = "release"
            version = "?"

        items.append(Text(text=f"DEX: {version}", on_click=lambda v: None))
        items.append(Divider())

        items.append(Selector(
            key="update_channel",
            text=_localize("update_channel"),
            icon="msg_channel",
            items=["Release", "Dev"],
            default=1 if channel == "dev" else 0,
            on_change=lambda v: self._on_channel_switch(v == 1)
        ))

        items.append(Text(
            text=_localize("select_version"),
            icon="msg_download",
            on_click=lambda v: self._on_select_version()
        ))

        items.append(Text(
            text=_localize("check_updates"),
            icon="msg_retry",
            on_click=lambda v: self._on_check_updates()
        ))
        items.append(Text(
            text=_localize("install_file"),
            icon="msg_folders",
            on_click=lambda v: self._on_install_file()
        ))
        
        items.append(Divider())
        
        items.append(Text(
            text=_localize("copy_logs"),
            icon="msg_copy",
            on_click=lambda v: self._on_copy_logs()
        ))
        items.append(Divider())
        items.append(Text(
            text=_localize("dex_settings"),
            icon="msg_settings",
            on_click=lambda v: self._open_re_extera_settings()
        ))

        return items

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
        local_dex_path = get_local_dex_path()
        if os.path.exists(local_dex_path):
            try:
                with open(local_dex_path, 'rb') as f:
                    dex_bytes = f.read()
                self.loader.start_from_bytes(dex_bytes)
                BulletinHelper.show_info(_localize("updated_cache"), get_last_fragment())
                self.log(f"Reloaded from local file {local_dex_path}")
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
                elif name.endswith("loader.plugin"):
                    plugin_url = asset.get("browser_download_url", "")
                    
            if dex_url:
                self._download_and_apply_version(tag, (dex_url, plugin_url))
            else:
                BulletinHelper.show_info("No DEX in this release", get_last_fragment())
                
        self._show_list_dialog("Select Release", labels, on_click)

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

    def on_plugin_load(self) -> None:
        try:
            self.log(f"Init {__version__}")
            launch_activity = get_last_fragment().getContext()
            self.loader = Loader(self, launch_activity)
            self.loader.load_and_start()
        except Exception as e:
            BulletinHelper.show_info(f"Error: {e}", get_last_fragment())
            self.log(f"Error: {e}")

    def on_plugin_unload(self) -> None:
        if self.loader is not None:
            self.loader.unload()