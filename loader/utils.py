"""Small runtime helpers: UI-thread dispatch and localization."""


class UIRunnable(dynamic_proxy(Runnable)):  # type: ignore
    """Wrap a Python callable as a Java ``Runnable`` for ``runOnUIThread``."""

    def __init__(self, func):
        super().__init__()
        self.func = func

    def run(self):
        self.func()


# Ordered as (ru, uk, en); see ``_localize`` for the index mapping.
_STRINGS = {
    "settings": [
        "Настройки re:extera",
        "Налаштування re:extera",
        "re:extera Settings"
    ],
    "channel_dev": [
        "Dev-сборки",
        "Dev-білди",
        "Dev builds"
    ],
    "channel_release": [
        "Релизные сборки",
        "Релізні білди",
        "Release builds"
    ],
    "check_updates": [
        "Проверить обновления",
        "Перевірити оновлення",
        "Check for updates"
    ],
    "install_file": [
        "Установить из файла",
        "Встановити з файлу",
        "Install from file"
    ],
    "update_avail": [
        "Доступна новая версия re:extera! Перезапустите приложение для применения обновления.",
        "Доступна нова версія re:extera! Перезапустіть додаток для застосування оновлення.",
        "New re:extera version available! Restart the app to apply the update."
    ],
    "downloading": [
        "Загрузка...",
        "Завантаження...",
        "Downloading..."
    ],
    "updated_cache": [
        "Обновлено из кеша",
        "Оновлено з кешу",
        "Updated from cache"
    ],
    "installed": [
        "Установка завершена",
        "Встановлення завершено",
        "Install completed"
    ],
    "channel_switch": [
        "Канал изменён. Перезапустите приложение.",
        "Канал змінено. Перезапустіть додаток.",
        "Channel changed. Restart the app."
    ],
    "up_to_date": [
        "Уже последняя версия",
        "Вже остання версія",
        "Already up to date"
    ],
    "file_not_found": [
        "Файл не найден",
        "Файл не знайдено",
        "File not found"
    ],
    "no_official_update": [
        "re:extera ещё не имеет официального обновления для этой версии, возможны баги.",
        "re:extera ще не має офіційного оновлення для цієї версії, можливі баги.",
        "re:extera doesn't have official update for this version, expect bugs."
    ],
    "export_logs": [
        "Экспортировать логи",
        "Експортувати логи",
        "Export logs"
    ],
    "logs_exported": [
        "Логи экспортированы",
        "Логи експортовано",
        "Logs exported"
    ],
    "update_channel": [
        "Канал обновлений",
        "Канал оновлень",
        "Update channel"
    ],
    "select_version": [
        "Выбрать версию",
        "Вибрати версію",
        "Select Version"
    ],
    "dex_settings": [
        "Настройки DEX",
        "Налаштування DEX",
        "DEX Settings"
    ],
}


def _localize(key):
    """Return ``key`` localized to the client's current language.

    Falls back to English for any language other than Russian or Ukrainian.
    """
    lang = LocaleController.getInstance().getCurrentLocale().getLanguage()
    idx = 0 if lang == "ru" else (1 if lang == "uk" else 2)
    return _STRINGS[key][idx]
