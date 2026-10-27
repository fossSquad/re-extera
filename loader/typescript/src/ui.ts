import { configManager } from './config'
import { dexLoader } from './dex'
import { t } from './i18n'
import { TAG } from './constants'
import { showBulletin } from './bulletin'

async function pickReleaseVersion(page: inu.ui.UIPage): Promise<void> {
  const releases = await dexLoader.fetchReleases()
  if (!releases || releases.length === 0) {
    showBulletin(t('no_releases'), { icon: inu.icons.animation('info'), duration: 'short' })
    return
  }

  const grouped = dexLoader.groupReleasesByTgVersion(releases)
  const tgVersions = Array.from(grouped.keys())
  if (tgVersions.length === 0) {
    showBulletin(t('no_releases'), { icon: inu.icons.animation('info'), duration: 'short' })
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

  showBulletin(t('downloading'), { icon: inu.icons.animation('loading'), duration: 'short' })
  const success = await dexLoader.downloadRelease(list[buildIdx])
  if (success) {
    showBulletin(t('update_avail'), { icon: inu.icons.animation('success'), duration: 'long' })
    page.invalidate()
  } else {
    showBulletin(t('download_failed'), { icon: inu.icons.animation('error'), duration: 'short' })
  }
}

async function pickDevBuild(page: inu.ui.UIPage): Promise<void> {
  const branches = await dexLoader.fetchBranches()
  if (!branches || branches.length === 0) {
    showBulletin(t('no_branches'), { icon: inu.icons.animation('info'), duration: 'short' })
    return
  }

  const branchIdx = await inu.ui.chooser({
    title: t('select_branch'),
    items: branches,
  })
  if (branchIdx == null) return

  const runs = await dexLoader.fetchRuns(branches[branchIdx])
  if (!runs || runs.length === 0) {
    showBulletin(t('no_runs'), { icon: inu.icons.animation('info'), duration: 'short' })
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

  showBulletin(t('downloading'), { icon: inu.icons.animation('loading'), duration: 'short' })
  const success = await dexLoader.downloadDevRun(runs[runIdx].id)
  if (success) {
    showBulletin(t('update_avail'), { icon: inu.icons.animation('success'), duration: 'long' })
    page.invalidate()
  } else {
    showBulletin(t('download_failed'), { icon: inu.icons.animation('error'), duration: 'short' })
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
          icon: inu.icons.common('info'),
          onClick: () => {
            showBulletin(`${verText} (${statusText})`, { icon: inu.icons.common('info'), duration: 'short' })
          },
        }),
        ui.separator(),
        ui.select({
          text: t('update_channel'),
          icon: inu.android.resourceIcon('msg_channel'),
          items: ['Release', 'Dev'],
          selected: channelIndex,
          onChange: async (index: number) => {
            const newChannel = index === 1 ? 'dev' : 'release'
            if (configManager.channel !== newChannel) {
              configManager.channel = newChannel
              await configManager.save()
              showBulletin(t('channel_switch'), { icon: inu.android.resourceIcon('msg_channel'), duration: 'long' })
              page.invalidate()
            }
          },
        }),
        ui.button({
          text: t('check_updates'),
          icon: inu.android.resourceIcon('msg_retry'),
          onClick: async () => {
            showBulletin(t('downloading'), { icon: inu.icons.animation('loading'), duration: 'short' })
            const updated = await dexLoader.checkAsyncUpdate(true)
            if (updated) {
              page.invalidate()
            } else {
              showBulletin(t('up_to_date'), { icon: inu.icons.animation('info'), duration: 'short' })
            }
          },
        }),
        ui.button({
          text: t('select_version'),
          icon: inu.android.resourceIcon('msg_download'),
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
          icon: inu.android.resourceIcon('msg_folders'),
          onClick: async () => {
            const success = await dexLoader.loadFromLocalFile()
            if (success) {
              showBulletin(t('updated_cache'), { icon: inu.icons.animation('success'), duration: 'short' })
              page.invalidate()
            } else {
              showBulletin(t('file_not_found'), { icon: inu.icons.animation('error'), duration: 'short' })
            }
          },
        }),
        ui.separator(),
        ui.button({
          text: t('copy_logs'),
          icon: inu.android.resourceIcon('msg_copy'),
          onClick: async () => {
            try {
              const javaLogs = dexLoader.getLogs()
              const fullLogs = `=== Inugram Loader Logs ===\n[TS Loader] Channel: ${configManager.channel}, Version: ${dexLoader.version}\n\n=== Hook Logs ===\n${javaLogs}`
              await inu.clipboard.write(fullLogs)
              showBulletin(t('logs_copied'), { icon: inu.icons.common('copy'), duration: 'short' })
            } catch (e) {
              console.error(TAG, 'Failed to copy logs:', e)
              showBulletin(`Error: ${e}`, { icon: inu.icons.animation('error'), duration: 'short' })
            }
          },
        }),
        ui.separator(),
        ui.button({
          text: t('dex_settings'),
          icon: inu.android.resourceIcon('msg_settings'),
          onClick: () => {
            dexLoader.showSettingsExternal()
          },
        }),
      ]
    },
  })

  inu.registerSettings(page)
}
