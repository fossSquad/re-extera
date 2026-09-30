import { TAG } from './constants'
import { configManager } from './config'
import { dexLoader } from './dex'
import { setupSettingsUI } from './ui'

async function init() {
  console.log(TAG, 'Initializing Inugram TypeScript loader...')

  // 1. Load config
  await configManager.load()

  // 2. Setup settings UI
  setupSettingsUI()

  // 3. Load and launch DEX
  const loaded = await dexLoader.loadFromCacheOrRemote()
  if (!loaded) {
    console.warn(TAG, 'Initial DEX load failed, will retry on next start or update check.')
  }
}

init().catch((e) => {
  console.error(TAG, 'Fatal error during initialization:', e)
})
