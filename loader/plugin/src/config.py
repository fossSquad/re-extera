import os
import json
import time

class Config:
    MIN_CHECK_INTERVAL = 60  # seconds between version checks

    def __init__(self, cache_dir):
        self.path = os.path.join(cache_dir, "config.json")
        self.data = self._load()

    def _load(self):
        try:
            if os.path.exists(self.path):
                with open(self.path, "r") as f:
                    return json.load(f)
        except Exception:
            pass
        return {"channel": "release", "versions": {}, "last_check": 0, "last_error": ""}

    def _save(self):
        try:
            d = os.path.dirname(self.path)
            if not os.path.exists(d):
                os.makedirs(d)
            with open(self.path, "w") as f:
                json.dump(self.data, f)
        except Exception as e:
            pass

    @property
    def channel(self):
        return self.data.get("channel", "release")

    @channel.setter
    def channel(self, value):
        self.data["channel"] = value
        self._save()

    def get_version(self, channel):
        return self.data.get("versions", {}).get(channel, 0)

    def set_version(self, channel, version):
        self.data.setdefault("versions", {})[channel] = version
        self._save()

    def clear_cache_for(self, channel):
        self.data.setdefault("versions", {})[channel] = 0
        self._save()

    def can_check(self):
        now = time.time()
        last = self.data.get("last_check", 0)
        return now - last >= self.MIN_CHECK_INTERVAL

    def mark_checked(self):
        self.data["last_check"] = time.time()
        self._save()