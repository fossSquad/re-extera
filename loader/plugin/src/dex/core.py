import os
from dalvik.system import InMemoryDexClassLoader, DexClassLoader
from java.nio import ByteBuffer
from org.telegram.messenger import ApplicationLoader
from hook_utils import find_class

from ..constants import (
    CLASS_NAME, DEX_OPT_DIR_NAME, LOCAL_DEX_PATH, VERSION_FIELD_NAME, CACHE_DIR_NAME
)

class CoreMixin:
    def _switch_cache(self, channel):
        self.channel = channel
        base_cache = os.path.join(
            ApplicationLoader.applicationContext.getFilesDir().getAbsolutePath(),
            CACHE_DIR_NAME
        )
        self.cache_dir = os.path.join(base_cache, channel)
        self.cache_file = os.path.join(self.cache_dir, "cached.dat")
        if not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)

    def getInstance(self):
        if self.instance is None:
            try:
                if getattr(self, "dex_main_class", None) is not None:
                    method = self.dex_main_class.getMethod("getInstance")
                    self.instance = method.invoke(None)
                else:
                    method = find_class(CLASS_NAME).getClass().getMethod("getInstance")
                    self.instance = method.invoke(None)
            except Exception as e:
                self.plugin.log(f"Error getting instance: {e}")
        return self.instance

    def _load_dex_inmemory(self, bytesdex):
        buffer = ByteBuffer.wrap(bytesdex)
        loader = InMemoryDexClassLoader(
            buffer, ApplicationLoader.applicationContext.getClassLoader()
        )
        clazz = loader.loadClass(CLASS_NAME)
        return clazz, loader

    def _load_dex_from_file(self, dex_path):
        loader = DexClassLoader(
            dex_path,
            ApplicationLoader.applicationContext.getDir(DEX_OPT_DIR_NAME, 0).getAbsolutePath(),
            None,
            ApplicationLoader.applicationContext.getClassLoader()
        )
        clazz = loader.loadClass(CLASS_NAME)
        return clazz, loader

    def _call_start(self, clazz):
        try:
            start_method = clazz.getMethod("initAndStart")
            start_method.invoke(None)
            self.plugin.log("Called initAndStart")
        except Exception as e:
            self.plugin.log(f"initAndStart failed: {e}")
            try:
                get_instance = clazz.getMethod("getInstance")
                instance = get_instance.invoke(None)
                start_method = clazz.getMethod("start")
                start_method.invoke(instance)
                self.plugin.log("Called start() via getInstance() fallback")
            except Exception as e2:
                self.plugin.log(f"Fallback start failed: {e2}")

    def start_from_bytes(self, bytesdex):
        cache_dir = self.cache_dir
        dex_path = os.path.join(cache_dir, "classes.dex")

        try:
            if not os.path.exists(cache_dir):
                os.makedirs(cache_dir)
            with open(dex_path, 'wb') as f:
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

        self.plugin.log(f"DEX proxy unavailable — hooks from static init may still work")
        self.instance = None

    def open_settings(self):
        try:
            inst = self.getInstance()
            if inst:
                method = inst.getClass().getMethod("showSettings")
                method.invoke(inst)
        except Exception as e:
            self.plugin.log(f"Error opening settings: {e}")

    def unload(self):
        try:
            inst = self.getInstance()
            if inst:
                method = inst.getClass().getMethod("onUnload")
                method.invoke(inst)
                self.plugin.log("Unloaded successfully")
        except Exception as e:
            self.plugin.log(f"Error unloading: {e}")

    def get_version_display(self):
        try:
            if getattr(self, "dex_main_class", None) is not None:
                v = self.dex_main_class.getDeclaredField(VERSION_FIELD_NAME)
                v.setAccessible(True)
                ver = str(v.get(None))
                return f"v{ver}"
            return "not loaded"
        except Exception:
            cached = self.config.get_version(self.channel)
            return f"cached:{cached}" if cached else "?"

    def _load_from_local_path(self):
        if os.path.exists(LOCAL_DEX_PATH):
            self.plugin.log(f"Local DEX found at {LOCAL_DEX_PATH}")
            with open(LOCAL_DEX_PATH, 'rb') as f:
                return f.read()
        return None

    def _load_from_cache(self):
        if os.path.exists(self.cache_file):
            with open(self.cache_file, 'rb') as f:
                return f.read()
        return None

    def load_and_start(self):
        self.plugin.log(f"Loading (channel: {self.channel})")

        local_bytes = self._load_from_local_path()
        if local_bytes is not None:
            try:
                self.start_from_bytes(local_bytes)
                self.plugin.log("Loaded from local storage")
                try:
                    self._check_async_update()
                except Exception as e:
                    self.plugin.log(f"Update check failed: {e}")
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
                try:
                    self._check_async_update()
                except Exception as e:
                    self.plugin.log(f"Update check failed: {e}")
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
                err = str(e)
                if "proxy" in err:
                    return
                self.plugin.log(f"Fatal error: {e}")
