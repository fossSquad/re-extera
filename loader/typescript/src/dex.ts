import {
  CACHE_DEX_DEV,
  CACHE_DEX_RELEASE,
  CLASS_NAME,
  DEV_API_URL,
  DEV_RUN_URL_TEMPLATE,
  RELEASE_API_URL,
  TAG,
  USER_AGENT,
} from './constants'
import { configManager } from './config'

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
    const cacheFile = this.getCacheFileName()
    try {
      if (inu.fs.exists(cacheFile)) {
        console.log(TAG, `Loading from cache: ${cacheFile}`)
        const bytes = inu.fs.read(cacheFile)
        const success = await this.startFromBytes(bytes)
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
      const bytes = inu.fs.read(localPath)
      const success = await this.startFromBytes(bytes)
      if (success) {
        this.loadedVersion = 'local'
      }
      return success
    } catch (e) {
      console.error(TAG, 'Failed to load local file:', e)
      return false
    }
  }

  async downloadLatest(): Promise<boolean> {
    if (configManager.channel === 'dev') {
      return await this.downloadLatestDev()
    } else {
      return await this.downloadLatestRelease()
    }
  }

  async downloadLatestRelease(): Promise<boolean> {
    try {
      console.log(TAG, 'Fetching latest release...')
      const res = await fetch(RELEASE_API_URL, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!res.ok) {
        console.error(TAG, 'Failed to fetch releases:', res.statusText)
        return false
      }

      const releases = (await res.json()) as any[]
      const currentTg = this.getTelegramVersion()

      let targetRelease = releases.find((r: any) => {
        const tag = r.tag_name || ''
        return currentTg && tag.endsWith(`-${currentTg}`)
      })

      if (!targetRelease && releases.length > 0) {
        targetRelease = releases[0]
      }

      if (!targetRelease) {
        console.error(TAG, 'No releases found')
        return false
      }

      const asset = targetRelease.assets?.find((a: any) => a.name === 'classes.dex')
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

      inu.fs.write(CACHE_DEX_RELEASE, bytes)
      this.setCachedVersion(targetRelease.tag_name)
      await configManager.save()

      const success = await this.startFromBytes(bytes)
      if (success) {
        this.loadedVersion = targetRelease.tag_name
      }
      return success
    } catch (e) {
      console.error(TAG, 'Failed to download release:', e)
      return false
    }
  }

  async downloadLatestDev(): Promise<boolean> {
    try {
      console.log(TAG, 'Fetching latest dev workflow run...')
      const res = await fetch(DEV_API_URL, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!res.ok) {
        console.error(TAG, 'Failed to fetch dev workflow runs:', res.statusText)
        return false
      }

      const data = (await res.json()) as any
      const runs = data.workflow_runs || []
      if (runs.length === 0) {
        console.error(TAG, 'No dev runs found')
        return false
      }

      const latestRun = runs[0]
      const runId = String(latestRun.id)
      const downloadUrl = DEV_RUN_URL_TEMPLATE.replace('{}', runId)

      console.log(TAG, `Downloading dev artifact for run #${runId}...`)
      const zipRes = await fetch(downloadUrl, {
        headers: { 'User-Agent': USER_AGENT },
      })
      if (!zipRes.ok) {
        console.error(TAG, 'Failed to download dev artifact:', zipRes.statusText)
        return false
      }

      const buffer = await zipRes.arrayBuffer()
      const zipBytes = new Uint8Array(buffer)
      const dexBytes = this.extractDexFromZip(zipBytes)

      if (!dexBytes || dexBytes.length === 0) {
        console.error(TAG, 'Could not extract classes.dex from dev zip')
        return false
      }

      inu.fs.write(CACHE_DEX_DEV, dexBytes)
      this.setCachedVersion(runId)
      await configManager.save()

      const success = await this.startFromBytes(dexBytes)
      if (success) {
        this.loadedVersion = `dev #${runId}`
      }
      return success
    } catch (e) {
      console.error(TAG, 'Failed to download dev run:', e)
      return false
    }
  }

  private extractDexFromZip(zipBytes: Uint8Array): Uint8Array | null {
    try {
      const ByteArrayInputStream = inu.jvm.cls('java.io.ByteArrayInputStream')
      const ZipInputStream = inu.jvm.cls('java.util.zip.ZipInputStream')
      const ByteArrayOutputStream = inu.jvm.cls('java.io.ByteArrayOutputStream')

      const bais = new ByteArrayInputStream(zipBytes)
      const zis = new ZipInputStream(bais)

      let entry = zis.call('getNextEntry') as JavaObject | null
      while (entry != null) {
        const name = entry.call('getName') as string
        if (name === 'classes.dex' || name.endsWith('/classes.dex')) {
          const baos = new ByteArrayOutputStream()
          const buffer = new Uint8Array(4096)
          let len = zis.call('read', buffer) as number
          while (len > 0) {
            baos.call('write', buffer, 0, len)
            len = zis.call('read', buffer) as number
          }
          zis.call('closeEntry')
          zis.call('close')
          return baos.call('toByteArray') as Uint8Array
        }
        entry = zis.call('getNextEntry') as JavaObject | null
      }
      zis.call('close')
    } catch (e) {
      console.error(TAG, 'Error extracting DEX from zip via Java:', e)
    }
    return null
  }

  async startFromBytes(bytes: Uint8Array): Promise<boolean> {
    try {
      console.log(TAG, `Loading DEX into Inugram (${bytes.length} bytes)...`)
      inu.jvm.loadDex(bytes)

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
      const mainClass = inu.jvm.cls(CLASS_NAME)
      mainClass.callStatic('showSettingsExternal')
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
