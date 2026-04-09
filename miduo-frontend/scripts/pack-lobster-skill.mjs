/**
 * Packs ticket-platform/docs/lobster-skill into public/lobster-skill.zip for static download (scheme A).
 */
import archiver from 'archiver'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createWriteStream } from 'node:fs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(__dirname, '..')
const sourceDir = path.resolve(frontendRoot, '../ticket-platform/docs/lobster-skill')
const publicDir = path.resolve(frontendRoot, 'public')
const outZip = path.join(publicDir, 'lobster-skill.zip')

if (!fs.existsSync(sourceDir)) {
  console.error('[pack-lobster-skill] 源目录不存在:', sourceDir)
  process.exit(1)
}

fs.mkdirSync(publicDir, { recursive: true })

const pkg = JSON.parse(fs.readFileSync(path.join(frontendRoot, 'package.json'), 'utf8'))
const versionTxt =
  `frontendPackageVersion=${pkg.version}\n` + `packedAt=${new Date().toISOString()}\n`

await new Promise((resolve, reject) => {
  const output = createWriteStream(outZip)
  const archive = archiver('zip', { zlib: { level: 9 } })

  output.on('close', resolve)
  output.on('error', reject)
  archive.on('error', reject)
  archive.on('warning', (err) => {
    if (err.code !== 'ENOENT') {
      reject(err)
    }
  })

  archive.pipe(output)
  archive.append(versionTxt, { name: 'lobster-skill/VERSION.txt' })
  archive.directory(sourceDir, 'lobster-skill')
  void archive.finalize()
})

console.log('[pack-lobster-skill] 已生成', outZip)
