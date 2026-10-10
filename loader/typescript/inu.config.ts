import { defineConfig } from '@inugram/cli'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '../..')

export default defineConfig({
  outDir: path.resolve(rootDir, 'build/plugin'),
  plugins: {
    loader: {
      entry: 'src/index.ts',
      manifest: {
        id: 're_extera_loader',
        name: "aartzz's re:extera",
        author: '@fossSquad | @shikaatuxplugins',
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
