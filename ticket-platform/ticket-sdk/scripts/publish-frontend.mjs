import { copyFile, mkdir } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const sdkRoot = resolve(scriptDir, '..')
const source = resolve(sdkRoot, 'dist/ticket-sdk.umd.js')
const target = resolve(sdkRoot, '../../miduo-frontend/public/sdk/v1/ticket-sdk.min.js')

await mkdir(dirname(target), { recursive: true })
await copyFile(source, target)

process.stdout.write(`Published SDK asset: ${target}\n`)
