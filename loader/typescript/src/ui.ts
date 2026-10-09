import { configManager } from './config'
import { dexLoader } from './dex'
import { t } from './i18n'
import { TAG } from './constants'

async function pickReleaseVersion(page: inu.ui.UIPage): Promise<void> {
  const releases = await dexLoader.fetchReleases()
  if (!releases || releases.length === 0) {
    inu.ui.toast(t('no_releases'))
    return
  }

  const grouped = dexLoader.groupReleasesByTgVersion(releases)
  const tgVersions = Array.from(grouped.keys())
  if (tgVersions.length === 0) {
    inu.ui.toast(t('no_releases'))
    return
  }

  const rawTg = dexLoader.getTelegramVersion().trim()
  const baseTg = rawTg.split('-')[0]
  let currentTg: string | null = null
  if (tgVersions.indexOf(rawTg) !== -1) {
    currentTg = rawTg
  } else if (tgVersions.indexOf(baseTg) !== -1) {
    currentTg = baseTg
  }
  if (currentTg) {
    tgVersions.splice(tgVersions.indexOf(currentTg), 1)
    tgVersions.unshift(currentTg)
  }

  const tgIdx = await inu.ui.chooser({
    title: t('select_tg_version'),
    items: tgVersions.map((v) => (v === currentTg ? `⭐ ${v}` : v)),
  })
  if (tgIdx == null) return

  const tgVersion = tgVersions[tgIdx]
  const list = grouped.get(tgVersion) || []
  const labels = list.map((rel) => (rel.tag_name || '').split('-')[0] || '?')
  if (labels.length > 0 && tgVersion === currentTg) {
    labels[0] = `⭐ ${labels[0]}`
  }

  const buildIdx = await inu.ui.chooser({
    title: t('select_release'),
    items: labels,
  })
  if (buildIdx == null) return

  inu.ui.toast(t('downloading'))
  const success = await dexLoader.downloadRelease(list[buildIdx])
  if (success) {
    inu.ui.toast(t('update_avail'))
    page.invalidate()
  } else {
    inu.ui.toast(t('download_failed'))
  }
}

async function pickDevBuild(page: inu.ui.UIPage): Promise<void> {
  const branches = await dexLoader.fetchBranches()
  if (!branches || branches.length === 0) {
    inu.ui.toast(t('no_branches'))
    return
  }

  const branchIdx = await inu.ui.chooser({
    title: t('select_branch'),
    items: branches,
  })
  if (branchIdx == null) return

  const runs = await dexLoader.fetchRuns(branches[branchIdx])
  if (!runs || runs.length === 0) {
    inu.ui.toast(t('no_runs'))
    return
  }

  const labels = runs.map((r) => `#${r.id} - ${(r.head_commit?.message || '').slice(0, 20)}`)
  if (labels.length > 0) {
    labels[0] = `⭐ ${labels[0]}`
  }

  const runIdx = await inu.ui.chooser({
    title: t('select_dev_build'),
    items: labels,
  })
  if (runIdx == null) return

  inu.ui.toast(t('downloading'))
  const success = await dexLoader.downloadDevRun(runs[runIdx].id)
  if (success) {
    inu.ui.toast(t('update_avail'))
    page.invalidate()
  } else {
    inu.ui.toast(t('download_failed'))
  }
}

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
          text: t('select_version'),
          onClick: async () => {
            if (configManager.channel === 'dev') {
              await pickDevBuild(page)
            } else {
              await pickReleaseVersion(page)
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
