#!/usr/bin/env node
/**
 * Whiten cream paper background only — no destructive zone erasure.
 * Badge / pill areas are covered in the Vue component via CSS overlays.
 */
import sharp from 'sharp'
import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..')

const DEFAULT_SOURCE = path.join(ROOT, 'public/mobile-factory/factory-poster-source.png')
const SOURCE = process.argv[2] || DEFAULT_SOURCE
const OUT_DIR = path.join(ROOT, 'public/mobile-factory')
const OUT_DISPLAY = path.join(OUT_DIR, 'factory-poster-header.png')

function whitenPaperBackground(buffer, width, height, channels) {
  const data = Buffer.from(buffer)
  for (let i = 0; i < data.length; i += channels) {
    const r = data[i]
    const g = data[i + 1]
    const b = data[i + 2]
    const max = Math.max(r, g, b)
    const min = Math.min(r, g, b)
    const sat = max - min
    const avg = (r + g + b) / 3

    const isPaper =
      avg > 228 &&
      sat < 28 &&
      r > 225 &&
      g > 222 &&
      b > 215 &&
      Math.abs(r - g) < 18 &&
      Math.abs(g - b) < 22

    if (isPaper) {
      data[i] = 255
      data[i + 1] = 255
      data[i + 2] = 255
    }
  }
  return data
}

function stripRedMarkup(buffer, width, height, channels) {
  const data = Buffer.from(buffer)
  for (let i = 0; i < data.length; i += channels) {
    const r = data[i]
    const g = data[i + 1]
    const b = data[i + 2]
    if (r > 210 && g < 90 && b < 90 && r - g > 120) {
      data[i] = 255
      data[i + 1] = 255
      data[i + 2] = 255
      if (channels === 4) data[i + 3] = 255
    }
  }
  return data
}

async function main() {
  await mkdir(OUT_DIR, { recursive: true })

  const meta = await sharp(SOURCE).metadata()
  const W = meta.width
  const H = meta.height
  console.log(`Source: ${SOURCE} (${W}x${H})`)

  let raw = await sharp(SOURCE).ensureAlpha().raw().toBuffer()
  raw = stripRedMarkup(raw, W, H, 4)
  raw = whitenPaperBackground(raw, W, H, 4)

  await sharp(raw, { raw: { width: W, height: H, channels: 4 } })
    .png()
    .toFile(OUT_DISPLAY)

  console.log(`Wrote ${OUT_DISPLAY}`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
