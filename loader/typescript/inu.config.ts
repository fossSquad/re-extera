export interface PluginConfig {
  entry: string
  outFile: string
  manifest: {
    id: string
    name: string
    author?: string
    version: string
    description?: string
    icon?: string
    grants?: string[]
  }
}

export function defineConfig(config: { plugins: Record<string, PluginConfig> }) {
  return config
}

import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '../..')

export default defineConfig({
  plugins: {
    re_extera: {
      entry: 'src/index.ts',
      outFile: path.resolve(rootDir, 'build/plugin/re_extera.inu.js'),
      manifest: {
        id: 're_extera_loader',
        name: "aartzz's re:extera",
        author: '@shiawasez | @shikaatuxplugins',
        version: '2.9.0',
        description: 'Actively maintained FOSS fork. Enable ghost mode, save deleted messages and more!',
        icon: 'inu://settings',
        grants: [
          'fetch',
          'fs',
          'clipboard.write',
          'unsafe.fs',
          'unsafe.jvm',
          'unsafe.xposed',
        ],
      },
    },
  },
})
