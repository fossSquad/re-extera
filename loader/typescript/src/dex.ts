import {
  BRANCHES_API_URL,
  CACHE_DEX_DIR,
  CACHE_DEX_DEV,
  CACHE_DEX_RELEASE,
  CLASS_NAME,
  DEV_RUN_URL_TEMPLATE,
  RELEASE_API_URL,
  RUNS_API_URL_TEMPLATE,
  TAG,
  USER_AGENT,
} from './constants'
import { configManager } from './config'

export interface GitHubRelease {
  tag_name?: string
  assets?: Array<{ name?: string; browser_download_url?: string }>
}

export interface DevRun {
  id: number
  head_commit?: { message?: string }
}

export class DexLoader {
  private dexLoaded = false
  private loadedVersion: string = '?'

  get version(): string {
    return this.loadedVersion
  }

  isLoaded(): boolean {
    return this.dexLoaded
  }

  getCacheFileName(): string {
    return configManager.channel === 'dev' ? CACHE_DEX_DEV : CACHE_DEX_RELEASE
  }

  getCachedVersion(): string | undefined {
    return configManager.channel === 'dev'
      ? configManager.cachedDevVersion
      : configManager.cachedReleaseVersion
  }

  setCachedVersion(ver: string | undefined): void {
    if (configManager.channel === 'dev') {
      configManager.cachedDevVersion = ver
    } else {
      configManager.cachedReleaseVersion = ver
    }
  }

  getLocalDexPath(): string {
    try {
      const AppLoader = inu.jvm.cls('org.telegram.messenger.ApplicationLoader')
      const ctx = AppLoader.getStaticField('applicationContext') as JavaObject
      const pkg = ctx.call('getPackageName') as string
      return `/storage/emulated/0/Android/media/${pkg}/classes.dex`
    } catch {
      return '/storage/emulated/0/Android/media/org.inugram.messenger/classes.dex'
    }
  }

  getTelegramVersion(): string {
    try {
      const BuildVars = inu.jvm.cls('org.telegram.messenger.BuildVars')
      return (BuildVars.getStaticField('BUILD_VERSION_STRING') as string) || ''
    } catch {
      return ''
    }
  }

  async loadFromCacheOrRemote(): Promise<boolean> {
    if (await this.loadFromLocalFile()) {
      console.log(TAG, 'Loaded DEX from local file')
      return true
    }

    const cacheFile = this.getCacheFileName()
    try {
      const cachePath = this.getDexCachePath(cacheFile)
      const File = inu.jvm.cls('java.io.File')
      const file = new File(cachePath)
      if (file.call('isFile')) {
        console.log(TAG, `Loading from cache: ${cacheFile}`)
        const success = await this.startFromPath(cachePath)
        if (success) {
          const cachedVer = this.getCachedVersion()
          if (cachedVer) {
            this.loadedVersion = cachedVer
          }
          return true
        }
      }
    } catch (e) {
      console.error(TAG, 'Failed to load from cache:', e)
    }

    return await this.downloadLatest()
  }

  async loadFromLocalFile(): Promise<boolean> {
    const localPath = this.getLocalDexPath()
    try {
      if (!inu.fs.exists(localPath)) {
        return false
      }
      const success = await this.copyAndStart(localPath, this.getCacheFileName())
      if (success) {
        this.loadedVersion = 'local'
      }
      return success
    } catch (e) {
      console.error(TAG, 'Failed to load local file:', e)
      return false
    }
  }

  removeLocalDex(): void {
    try {
      const localPath = this.getLocalDexPath()
      if (inu.fs.exists(localPath)) {
        inu.fs.rm(localPath)
        console.log(TAG, 'Removed local DEX because a new version was explicitly downloaded')
      }
    } catch (e) {
      console.error(TAG, 'Failed to remove local DEX:', e)
    }
  }

  async downloadLatest(): Promise<boolean> {
    if (configManager.channel === 'dev') {
      return await this.downloadLatestDev()
    } else {
      return await this.downloadLatestRelease()
    }
  }

  async fetchReleases(): Promise<GitHubRelease[] | null> {
    try {
      console.log(TAG, 'Fetching releases list...')
      const res = await fetch(RELEASE_API_URL, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!res.ok) {
        console.error(TAG, 'Failed to fetch releases:', res.statusText)
        return null
      }
      return (await res.json()) as GitHubRelease[]
    } catch (e) {
      console.error(TAG, 'Failed to fetch releases:', e)
      return null
    }
  }

  groupReleasesByTgVersion(releases: GitHubRelease[]): Map<string, GitHubRelease[]> {
    const grouped = new Map<string, GitHubRelease[]>()
    for (const rel of releases) {
      const tag = rel.tag_name || ''
      const dash = tag.lastIndexOf('-')
      if (dash === -1) continue
      const tgVersion = tag.slice(dash + 1)
      const list = grouped.get(tgVersion)
      if (list) {
        list.push(rel)
      } else {
        grouped.set(tgVersion, [rel])
      }
    }
    return grouped
  }

  async downloadRelease(targetRelease: GitHubRelease): Promise<boolean> {
    try {
      const asset = targetRelease.assets?.find((a) => a.name?.endsWith('.dex'))
      if (!asset?.browser_download_url) {
        console.error(TAG, 'classes.dex not found in release assets')
        return false
      }

      console.log(TAG, `Downloading release ${targetRelease.tag_name} from ${asset.browser_download_url}...`)
      const dexRes = await fetch(asset.browser_download_url, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!dexRes.ok) {
        console.error(TAG, 'Failed to download dex:', dexRes.statusText)
        return false
      }

      const buffer = await dexRes.arrayBuffer()
      const bytes = new Uint8Array(buffer)

      const success = await this.cacheAndStart(bytes, CACHE_DEX_RELEASE)
      if (success) {
        this.removeLocalDex()
        this.setCachedVersion(targetRelease.tag_name)
        await configManager.save()
        this.loadedVersion = targetRelease.tag_name || '?'
      }
      return success
    } catch (e) {
      console.error(TAG, 'Failed to download release:', e)
      return false
    }
  }

  async downloadLatestRelease(): Promise<boolean> {
    console.log(TAG, 'Fetching latest release...')
    const releases = await this.fetchReleases()
    if (!releases || releases.length === 0) {
      console.error(TAG, 'No releases found')
      return false
    }

    const currentTg = this.getTelegramVersion()
    let targetRelease = releases.find((r) => {
      const tag = r.tag_name || ''
      return currentTg && tag.endsWith(`-${currentTg}`)
    })

    if (!targetRelease) {
      targetRelease = releases[0]
    }

    return await this.downloadRelease(targetRelease)
  }

  async fetchBranches(): Promise<string[] | null> {
    try {
      console.log(TAG, 'Fetching branches list...')
      const res = await fetch(BRANCHES_API_URL, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!res.ok) {
        console.error(TAG, 'Failed to fetch branches:', res.statusText)
        return null
      }
      const data = (await res.json()) as Array<{ name?: string }>
      return data.map((b) => b.name || '').filter((name) => name.length > 0)
    } catch (e) {
      console.error(TAG, 'Failed to fetch branches:', e)
      return null
    }
  }

  async fetchRuns(branch: string): Promise<DevRun[] | null> {
    try {
      console.log(TAG, `Fetching workflow runs for branch ${branch}...`)
      const url = RUNS_API_URL_TEMPLATE.replace('{}', encodeURIComponent(branch))
      const res = await fetch(url, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!res.ok) {
        console.error(TAG, 'Failed to fetch dev workflow runs:', res.statusText)
        return null
      }
      const data = (await res.json()) as { workflow_runs?: DevRun[] }
      return data.workflow_runs || []
    } catch (e) {
      console.error(TAG, 'Failed to fetch dev workflow runs:', e)
      return null
    }
  }

  async downloadDevRun(runId: number | string): Promise<boolean> {
    try {
      const id = String(runId)
      const downloadUrl = DEV_RUN_URL_TEMPLATE.replace('{}', id)

      console.log(TAG, `Downloading dev artifact for run #${id}...`)
      const zipRes = await fetch(downloadUrl, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!zipRes.ok) {
        console.error(TAG, 'Failed to download dev artifact:', zipRes.statusText)
        return false
      }

      const buffer = await zipRes.arrayBuffer()
      const zipBytes = new Uint8Array(buffer)
      const success = await this.extractAndStart(zipBytes, CACHE_DEX_DEV)
      if (success) {
        this.removeLocalDex()
        this.setCachedVersion(id)
        await configManager.save()
        this.loadedVersion = `dev #${id}`
      }
      return success
    } catch (e) {
      console.error(TAG, 'Failed to download dev run:', e)
      return false
    }
  }

  async downloadLatestDev(): Promise<boolean> {
    console.log(TAG, 'Fetching latest dev workflow run...')
    const runs = await this.fetchRuns('master')
    if (!runs || runs.length === 0) {
      console.error(TAG, 'No dev runs found')
      return false
    }
    return await this.downloadDevRun(runs[0].id)
  }

  private extractAndStart(zipBytes: Uint8Array, fileName: string): Promise<boolean> {
    try {
      const cachePath = this.getDexCachePath(fileName)
      const ByteArrayInputStream = inu.jvm.cls('java.io.ByteArrayInputStream')
      const ZipInputStream = inu.jvm.cls('java.util.zip.ZipInputStream')
      const FileOutputStream = inu.jvm.cls('java.io.FileOutputStream')

      this.prepareDexCache(cachePath)
      const bais = new ByteArrayInputStream(zipBytes)
      const zis = new ZipInputStream(bais)

      try {
        let entry = zis.call('getNextEntry') as JavaObject | null
        while (entry != null) {
          const name = entry.call('getName') as string
          if (name === 'classes.dex' || name.endsWith('/classes.dex')) {
            const output = new FileOutputStream(cachePath, false)
            try {
              const copied = zis.call('transferTo', output) as number
              if (copied <= 0) {
                throw new Error('Extracted classes.dex is empty')
              }
            } finally {
              output.call('close')
            }
            this.makeDexReadOnly(cachePath)
            return this.startFromPath(cachePath)
          }
          entry = zis.call('getNextEntry') as JavaObject | null
        }
      } finally {
        zis.call('close')
      }
    } catch (e) {
      console.error(TAG, 'Error extracting DEX from zip via Java:', e)
    }
    console.error(TAG, 'Could not extract classes.dex from dev zip')
    return Promise.resolve(false)
  }

  private getDexCachePath(fileName: string): string {
    const AppLoader = inu.jvm.cls('org.telegram.messenger.ApplicationLoader')
    const ctx = AppLoader.getStaticField('applicationContext') as JavaObject
    const filesDir = ctx.call('getCodeCacheDir') as JavaObject
    return `${filesDir.call('getAbsolutePath') as string}/${CACHE_DEX_DIR}/${fileName}`
  }

  private async cacheAndStart(bytes: Uint8Array, fileName: string): Promise<boolean> {
    try {
      const cachePath = this.getDexCachePath(fileName)
      const FileOutputStream = inu.jvm.cls('java.io.FileOutputStream')
      this.prepareDexCache(cachePath)
      const output = new FileOutputStream(cachePath, false)
      try {
        output.call('write', bytes)
      } finally {
        output.call('close')
      }
      this.makeDexReadOnly(cachePath)
      return await this.startFromPath(cachePath)
    } catch (e) {
      console.error(TAG, 'Failed to cache DEX:', e)
      return false
    }
  }

  private async copyAndStart(sourcePath: string, fileName: string): Promise<boolean> {
    try {
      const cachePath = this.getDexCachePath(fileName)
      const File = inu.jvm.cls('java.io.File')
      const FileInputStream = inu.jvm.cls('java.io.FileInputStream')
      const FileOutputStream = inu.jvm.cls('java.io.FileOutputStream')
      this.prepareDexCache(cachePath)

      const input = new FileInputStream(sourcePath)
      const output = new FileOutputStream(cachePath, false)
      try {
        const inputChannel = input.call('getChannel') as JavaObject
        const outputChannel = output.call('getChannel') as JavaObject
        const sourceSize = inputChannel.call('size') as number
        const copied = inputChannel.call('transferTo', 0, sourceSize, outputChannel) as number
        if (copied !== sourceSize) {
          throw new Error(`Copied DEX size does not match source: ${copied} != ${sourceSize}`)
        }
      } finally {
        output.call('close')
        input.call('close')
      }

      const source = new File(sourcePath)
      const cached = new File(cachePath)
      if (source.call('length') !== cached.call('length')) {
        throw new Error(`Copied DEX size does not match source: ${source.call('length')} != ${cached.call('length')}`)
      }
      this.makeDexReadOnly(cachePath)
      return await this.startFromPath(cachePath)
    } catch (e) {
      console.error(TAG, 'Failed to copy local DEX:', e)
      return false
    }
  }

  private prepareDexCache(path: string): void {
    const File = inu.jvm.cls('java.io.File')
    const file = new File(path)
    const parent = file.call('getParentFile') as JavaObject
    if (!parent.call('isDirectory') && !parent.call('mkdirs')) {
      throw new Error(`Could not create DEX cache directory: ${parent.call('getAbsolutePath')}`)
    }
    if (file.call('exists')) {
      file.call('setWritable', true)
      if (!file.call('delete')) {
        throw new Error(`Could not replace DEX cache: ${path}`)
      }
    }
  }

  private makeDexReadOnly(path: string): void {
    const File = inu.jvm.cls('java.io.File')
    const file = new File(path)
    if (!file.call('setReadOnly')) {
      throw new Error(`Could not mark DEX cache read-only: ${path}`)
    }
  }

  private async startFromPath(path: string): Promise<boolean> {
    try {
      console.log(TAG, `Loading DEX into Inugram from ${path}...`)
      inu.jvm.loadDex(path)

      const mainClass = inu.jvm.cls(CLASS_NAME)
      if (!mainClass) {
        console.error(TAG, `Class not found: ${CLASS_NAME}`)
        return false
      }

      console.log(TAG, 'Calling initAndStart()...')
      mainClass.callStatic('initAndStart')
      this.dexLoaded = true

      try {
        const ver = mainClass.getStaticField('VERSION') as string
        if (ver) {
          this.loadedVersion = ver
        }
      } catch {
        // ignore
      }

      console.log(TAG, `re:extera initialized successfully! Version: ${this.loadedVersion}`)
      return true
    } catch (e) {
      console.error(TAG, 'Failed to start DEX:', e)
      return false
    }
  }

  showSettingsExternal(): void {
    try {
      const Handler = inu.jvm.cls('android.os.Handler')
      const Looper = inu.jvm.cls('android.os.Looper')
      const mainClass = inu.jvm.cls(CLASS_NAME)
      const handler = new Handler(Looper.callStatic('getMainLooper'))
      handler.call('post', inu.jvm.runnable(() => {
        try {
          mainClass.callStatic('showSettingsExternal')
        } catch (e) {
          console.error(TAG, 'Failed to open re:extera settings:', e)
        }
      }))
    } catch (e) {
      console.error(TAG, 'Failed to open re:extera settings:', e)
    }
  }

  getLogs(): string {
    try {
      const mainClass = inu.jvm.cls(CLASS_NAME)
      return (mainClass.callStatic('getLogs') as string) || ''
    } catch (e) {
      return `Failed to fetch Java logs: ${e}`
    }
  }
}

export const dexLoader = new DexLoader()
