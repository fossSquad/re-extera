import { CONFIG_FILE, TAG } from './constants'

export type UpdateChannel = 'release' | 'dev'

export interface LoaderConfig {
  channel: UpdateChannel
  cachedReleaseVersion?: string
  cachedDevVersion?: string
  lastCheck?: number
}

const defaultConfig: LoaderConfig = {
  channel: 'release',
}

export class ConfigManager {
  private config: LoaderConfig = { ...defaultConfig }

  async load(): Promise<LoaderConfig> {
    try {
      if (inu.fs.exists(CONFIG_FILE)) {
        const bytes = inu.fs.read(CONFIG_FILE)
        const text = new TextDecoder().decode(bytes)
        this.config = { ...defaultConfig, ...JSON.parse(text) }
      }
    } catch (e) {
      console.error(TAG, 'Failed to load config:', e)
    }
    return this.config
  }

  async save(): Promise<void> {
    try {
      const text = JSON.stringify(this.config, null, 2)
      const bytes = new TextEncoder().encode(text)
      inu.fs.write(CONFIG_FILE, bytes)
    } catch (e) {
      console.error(TAG, 'Failed to save config:', e)
    }
  }

  get channel(): UpdateChannel {
    return this.config.channel
  }

  set channel(value: UpdateChannel) {
    this.config.channel = value
  }

  get cachedReleaseVersion(): string | undefined {
    return this.config.cachedReleaseVersion
  }

  set cachedReleaseVersion(val: string | undefined) {
    this.config.cachedReleaseVersion = val
  }

  get cachedDevVersion(): string | undefined {
    return this.config.cachedDevVersion
  }

  set cachedDevVersion(val: string | undefined) {
    this.config.cachedDevVersion = val
  }

  get lastCheck(): number | undefined {
    return this.config.lastCheck
  }

  set lastCheck(val: number | undefined) {
    this.config.lastCheck = val
  }
}

export const configManager = new ConfigManager()
