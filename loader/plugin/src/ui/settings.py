from typing import List, Any
from ui.settings import Text, Divider, Selector
from ..utils import _localize

class SettingsMixin:
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
