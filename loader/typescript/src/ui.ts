import { configManager } from './config'
import { dexLoader } from './dex'
import { t } from './i18n'
import { TAG } from './constants'

export function setupSettingsUI(): void {
  const { ui } = inu

  const page = ui.settingsPage({
    title: t('settings'),
    items: () => {
      const channelIndex = configManager.channel === 'dev' ? 1 : 0
      const verText = `DEX: ${dexLoader.version}`
      const statusText = dexLoader.isLoaded() ? 'Loaded' : 'Not Loaded'

      return [
        ui.header('re:extera'),
        ui.button({
          text: verText,
          subtitle: statusText,
          onClick: () => {
            ui.toast(`${verText} (${statusText})`)
          },
        }),
        ui.separator(),
        ui.select({
          text: t('update_channel'),
          items: ['Release', 'Dev'],
          selected: channelIndex,
          onChange: async (index: number) => {
            const newChannel = index === 1 ? 'dev' : 'release'
            if (configManager.channel !== newChannel) {
              configManager.channel = newChannel
              await configManager.save()
              ui.toast(t('channel_switch'))
              page.invalidate()
            }
          },
        }),
        ui.button({
          text: t('check_updates'),
          onClick: async () => {
            ui.toast(t('downloading'))
            const success = await dexLoader.downloadLatest()
            if (success) {
              ui.toast(t('update_avail'))
              page.invalidate()
            } else {
              ui.toast(t('up_to_date'))
            }
          },
        }),
        ui.button({
          text: t('install_file'),
          onClick: async () => {
            const success = await dexLoader.loadFromLocalFile()
            if (success) {
              ui.toast(t('updated_cache'))
              page.invalidate()
            } else {
              ui.toast(t('file_not_found'))
            }
          },
        }),
        ui.separator(),
        ui.button({
          text: t('copy_logs'),
          onClick: async () => {
            try {
              const javaLogs = dexLoader.getLogs()
              const fullLogs = `=== Inugram Loader Logs ===\n[TS Loader] Channel: ${configManager.channel}, Version: ${dexLoader.version}\n\n=== Hook Logs ===\n${javaLogs}`
              await inu.clipboard.write(fullLogs)
              ui.toast(t('logs_copied'))
            } catch (e) {
              console.error(TAG, 'Failed to copy logs:', e)
              ui.toast(`Error: ${e}`)
            }
          },
        }),
        ui.separator(),
        ui.button({
          text: t('dex_settings'),
          onClick: () => {
            dexLoader.showSettingsExternal()
          },
        }),
      ]
    },
  })

  inu.registerSettings(page)
}
