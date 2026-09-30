import { existsSync, mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import * as esbuild from 'esbuild'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectRoot = path.resolve(__dirname, '../..')
const outputDir = path.resolve(projectRoot, 'build/plugin')
const outputFile = path.resolve(outputDir, 're_extera.inu.js')

mkdirSync(outputDir, { recursive: true })

const inuCliPath = process.env.INU_CLI_PATH
let builtWithCli = false

if (inuCliPath && existsSync(inuCliPath)) {
  console.log(`Using Inugram CLI from sources: ${inuCliPath}`)
  const res = spawnSync('node', [inuCliPath, 'build'], {
    cwd: __dirname,
    stdio: 'inherit',
  })
  if (res.status === 0) {
    builtWithCli = true
  } else {
    console.warn(`Inugram CLI build exited with code ${res.status}, falling back to standalone esbuild`)
  }
}

if (!builtWithCli) {
  console.log('Building Inugram loader with standalone esbuild...')
  const banner = `// ==InuPlugin==
// @id          re_extera_loader
// @name        aartzz's re:extera
// @version     2.9.0
// @author      @shiawasez | @shikaatuxplugins
// @description Actively maintained FOSS fork. Enable ghost mode, save deleted messages and more!
// @icon        inu://settings
// @grant       fetch
// @grant       fs
// @grant       clipboard.write
// @grant       unsafe.fs
// @grant       unsafe.jvm
// @grant       unsafe.xposed
// ==/InuPlugin==
`

  await esbuild.build({
    entryPoints: [path.resolve(__dirname, 'src/index.ts')],
    outfile: outputFile,
    bundle: true,
    format: 'esm',
    target: 'esnext',
    platform: 'neutral',
    charset: 'utf8',
    banner: {
      js: banner,
    },
    sourcemap: false,
    minify: false,
  })

  console.log(`Successfully generated ${path.relative(projectRoot, outputFile)}`)
}
