export type TranslationKey =
  | 'settings'
  | 'channel_dev'
  | 'channel_release'
  | 'check_updates'
  | 'install_file'
  | 'update_avail'
  | 'downloading'
  | 'updated_cache'
  | 'installed'
  | 'channel_switch'
  | 'up_to_date'
  | 'file_not_found'
  | 'no_official_update'
  | 'copy_logs'
  | 'logs_copied'
  | 'update_channel'
  | 'select_version'
  | 'dex_settings'

const strings: Record<TranslationKey, [string, string, string]> = {
  settings: ['Настройки re:extera', 'Налаштування re:extera', 're:extera Settings'],
  channel_dev: ['Dev-сборки', 'Dev-білди', 'Dev builds'],
  channel_release: ['Релизные сборки', 'Релізні білди', 'Release builds'],
  check_updates: ['Проверить обновления', 'Перевірити оновлення', 'Check for updates'],
  install_file: ['Установить из файла', 'Встановити з файлу', 'Install from file'],
  update_avail: [
    'Доступна новая версия re:extera! Перезапустите приложение для применения обновления.',
    'Доступна нова версія re:extera! Перезапустіть додаток для застосування оновлення.',
    'New re:extera version available! Restart the app to apply the update.',
  ],
  downloading: ['Загрузка...', 'Завантаження...', 'Downloading...'],
  updated_cache: ['Обновлено из кеша', 'Оновлено з кешу', 'Updated from cache'],
  installed: ['Установка завершена', 'Встановлення завершено', 'Install completed'],
  channel_switch: [
    'Канал изменён. Перезапустите приложение.',
    'Канал змінено. Перезапустіть додаток.',
    'Channel changed. Restart the app.',
  ],
  up_to_date: ['Уже последняя версия', 'Вже остання версія', 'Already up to date'],
  file_not_found: ['Файл не найден', 'Файл не знайдено', 'File not found'],
  no_official_update: [
    're:extera ещё не имеет официального обновления для этой версии, возможны баги.',
    're:extera ще не має офіційного оновлення для цієї версії, можливі баги.',
    "re:extera doesn't have official update for this version, expect bugs.",
  ],
  copy_logs: ['Скопировать логи', 'Скопіювати логи', 'Copy logs'],
  logs_copied: [
    'Логи скопированы в буфер обмена',
    'Логи скопійовано в буфер обміну',
    'Logs copied to clipboard',
  ],
  update_channel: ['Канал обновлений', 'Канал оновлень', 'Update channel'],
  select_version: ['Выбрать версию', 'Вибрати версію', 'Select Version'],
  dex_settings: ['Настройки DEX', 'Налаштування DEX', 'DEX Settings'],
}

export function getLanguage(): string {
  try {
    const LocaleController = inu.jvm.cls('org.telegram.messenger.LocaleController')
    const instance = LocaleController.callStatic('getInstance') as JavaObject
    const locale = instance.call('getCurrentLocale') as JavaObject
    const lang = locale.call('getLanguage') as string
    return lang || 'en'
  } catch {
    return 'en'
  }
}

export function t(key: TranslationKey): string {
  const lang = getLanguage()
  const idx = lang === 'ru' ? 0 : lang === 'uk' ? 1 : 2
  const entry = strings[key]
  return entry ? entry[idx] : key
}
