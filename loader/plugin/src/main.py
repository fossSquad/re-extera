from base_plugin import BasePlugin
from .core import CorePluginMixin
from .ui import SettingsMixin, ActionsMixin, VersionSelectorMixin

class Plugin(CorePluginMixin, SettingsMixin, ActionsMixin, VersionSelectorMixin, BasePlugin):
    pass
