"""Persistent loader configuration (selected channel, cached versions, rate limit)."""


class Config:
    """JSON-backed settings stored under the loader cache directory."""

    MIN_CHECK_INTERVAL = 60  # minimum seconds between remote version checks

    def __init__(self, cache_dir):
        self.path = os.path.join(cache_dir, "config.json")
        self.data = self._load()

    def _load(self):
        try:
            if os.path.exists(self.path):
                with open(self.path) as f:
                    return json.load(f)
        except Exception:
            pass
        return {"channel": "release", "versions": {}, "last_check": 0, "last_error": ""}

    def _save(self):
        try:
            os.makedirs(os.path.dirname(self.path), exist_ok=True)
            with open(self.path, "w") as f:
                json.dump(self.data, f)
        except Exception:
            pass

    @property
    def channel(self):
        return self.data.get("channel", "release")

    @channel.setter
    def channel(self, value):
        self.data["channel"] = value
        self._save()

    def get_version(self, channel):
        versions = self.data.get("versions")
        if not isinstance(versions, dict):
            return 0
        return versions.get(channel, 0)

    def set_version(self, channel, version):
        if not isinstance(self.data.get("versions"), dict):
            self.data["versions"] = {}
        self.data["versions"][channel] = version
        self._save()

    def can_check(self):
        return time.time() - self.data.get("last_check", 0) >= self.MIN_CHECK_INTERVAL

    def mark_checked(self):
        self.data["last_check"] = time.time()
        self._save()
