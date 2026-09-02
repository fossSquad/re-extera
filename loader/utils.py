class UIRunnable(dynamic_proxy(Runnable)):
    def __init__(self, func):
        super().__init__()
        self.func = func
    def run(self):
        self.func()


def get_local_dex_path():
    pkg = ApplicationLoader.applicationContext.getPackageName()
    return f"/storage/emulated/0/Android/media/{pkg}/classes.dex"



# localization
def _localize(key):
    lang = LocaleController.getInstance().getCurrentLocale().getLanguage()
    strings = {
        "settings":        ("Настройки re:extera",  "Налаштування re:extera",  "re:extera Settings"),
        "channel_dev":     ("Dev-сборки",            "Dev-білди",               "Dev builds"),
        "channel_release": ("Релизные сборки",       "Релізні білди",           "Release builds"),
        "check_updates":   ("Проверить обновления",  "Перевірити оновлення",    "Check for updates"),
        "install_file":    ("Установить из файла",   "Встановити з файлу",      "Install from file"),
        "update_avail":    ("Доступна новая версия re:extera! Перезапустите приложение для применения обновления.",
                            "Доступна нова версія re:extera! Перезапустіть додаток для застосування оновлення.",
                            "New re:extera version available! Restart the app to apply the update."),
        "downloading":     ("Загрузка...",           "Завантаження...",         "Downloading..."),
        "updated_cache":   ("Обновлено из кеша",     "Оновлено з кешу",         "Updated from cache"),
        "installed":       ("Установка завершена",   "Встановлення завершено",  "Install completed"),
        "channel_switch":  ("Канал изменён. Перезапустите приложение.", "Канал змінено. Перезапустіть додаток.", "Channel changed. Restart the app."),
        "up_to_date":      ("Уже последняя версия",  "Вже остання версія",      "Already up to date"),
        "file_not_found":  ("Файл не найден",        "Файл не знайдено",        "File not found"),
        "no_official_update": ("re:extera ещё не имеет официального обновления для этой версии, возможны баги.", 
                               "re:extera ще не має офіційного оновлення для цієї версії, можливі баги.",
                               "re:extera doesn't have official update for this version, expect bugs."),
        "copy_logs":       ("Скопировать логи",      "Скопіювати логи",         "Copy logs"),
        "logs_copied":     ("Логи скопированы в буфер обмена", "Логи скопійовано в буфер обміну", "Logs copied to clipboard"),
        "update_channel":  ("Канал обновлений",      "Канал оновлень",          "Update channel"),
        "select_version":  ("Выбрать версию",        "Вибрати версію",          "Select Version"),
        "dex_settings":    ("Настройки DEX",         "Налаштування DEX",        "DEX Settings"),
    }
    idx = 0 if lang == "ru" else (1 if lang == "uk" else 2)
    return strings[key][idx]


# file download handler
class DownloadListener(dynamic_proxy(NotificationCenter.NotificationCenterDelegate)):
    def __init__(self, plugin, account, file_name, download_path, target_path):
        super().__init__()
        self.plugin = plugin
        self.account = account
        self.file_name = file_name
        self.download_path = download_path
        self.target_path = target_path
        self.nc = NotificationCenter.getInstance(account)

    def didReceivedNotification(self, id, account, args):
        try:
            loaded_file_name = str(args[0])
            if loaded_file_name == self.file_name:
                if id == NotificationCenter.fileLoaded:
                    self.plugin.log(f"File {self.file_name} downloaded success")
                    if os.path.exists(self.download_path):
                        shutil.copy2(self.download_path, self.target_path)
                        self.plugin.log(f"Copied to {self.target_path}")
                        BulletinHelper.show_info(_localize("installed"))
                    self.cleanup()
                elif id == NotificationCenter.fileLoadFailed:
                    self.plugin.log(f"File {self.file_name} download failed")
                    self.cleanup()
        except Exception as e:
            self.plugin.log(f"Listener error: {e}")

    def cleanup(self):
        self.nc.removeObserver(self, NotificationCenter.fileLoaded)
        self.nc.removeObserver(self, NotificationCenter.fileLoadFailed)