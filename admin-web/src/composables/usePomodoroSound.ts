let audioCtx: AudioContext | null = null

/**
 * 生成 PCM 16-bit 单声道 WAV 的 Blob URL
 * 不依赖 AudioContext，在 HTTP 环境下也能正常播放
 */
let wavUrlCache = new Map<string, string>()

function getWavUrl(frequency: number): string {
  const key = String(frequency)
  if (wavUrlCache.has(key)) return wavUrlCache.get(key)!

  const sampleRate = 22050
  const durationSec = 0.18
  const numSamples = Math.floor(sampleRate * durationSec)

  // WAV header (44 bytes) + data
  const buf = new ArrayBuffer(44 + numSamples * 2)
  const dv = new DataView(buf)

  const writeStr = (off: number, s: string) => {
    for (let i = 0; i < s.length; i++) dv.setUint8(off + i, s.charCodeAt(i))
  }

  writeStr(0, 'RIFF')
  dv.setUint32(4, 36 + numSamples * 2, true)
  writeStr(8, 'WAVE')
  writeStr(12, 'fmt ')
  dv.setUint32(16, 16, true)       // subchunk1 size
  dv.setUint16(20, 1, true)        // PCM
  dv.setUint16(22, 1, true)        // mono
  dv.setUint32(24, sampleRate, true)
  dv.setUint32(28, sampleRate * 2, true) // byte rate
  dv.setUint16(32, 2, true)        // block align
  dv.setUint16(34, 16, true)       // bits per sample
  writeStr(36, 'data')
  dv.setUint32(40, numSamples * 2, true)

  // 写入方波采样（近似 AudioContext 的 square 波形）
  for (let i = 0; i < numSamples; i++) {
    const t = i / sampleRate
    // square wave: sign of sin
    const val = Math.sin(2 * Math.PI * frequency * t) >= 0 ? 0.5 : -0.5
    dv.setInt16(44 + i * 2, val * 32767 * 0.7, true)
  }

  const blob = new Blob([buf], { type: 'audio/wav' })
  const url = URL.createObjectURL(blob)
  wavUrlCache.set(key, url)
  return url
}

function playFallbackBeeps(frequency: number, beeps: number, volume = 0.3) {
  const url = getWavUrl(frequency)
  let played = 0

  function playNext() {
    if (played >= beeps) return
    played++
    const audio = new Audio(url)
    audio.volume = volume
    audio.play().catch(() => {
      // 静默忽略，可能被浏览器拦截
    })
    setTimeout(playNext, 400)
  }

  playNext()
}

function ensureAudioContext(): Promise<AudioContext | null> {
  return new Promise((resolve) => {
    if (audioCtx) {
      if (audioCtx.state === 'suspended') {
        audioCtx.resume()
          .then(() => resolve(audioCtx))
          .catch(() => {
            audioCtx = null
            resolve(null)
          })
      } else {
        resolve(audioCtx)
      }
      return
    }

    try {
      const Adapter = (window as any).AudioContext || (window as any).webkitAudioContext
      if (!Adapter) {
        resolve(null)
        return
      }
      audioCtx = new Adapter()
      resolve(audioCtx)
    } catch {
      resolve(null)
    }
  })
}

function playNote(
  ctx: AudioContext,
  frequency: number,
  duration: number,
  type: OscillatorType = 'square',
  volume = 0.3,
) {
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = type
  osc.frequency.setValueAtTime(frequency, ctx.currentTime)
  gain.gain.setValueAtTime(volume, ctx.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration)
  osc.connect(gain)
  gain.connect(ctx.destination)
  osc.start(ctx.currentTime)
  osc.stop(ctx.currentTime + duration)
}

function playBeeps(
  ctx: AudioContext,
  frequency: number,
  beeps: number,
  volume: number,
  type: OscillatorType = 'square',
) {
  for (let i = 0; i < beeps; i++) {
    const t = i * 0.4
    setTimeout(() => playNote(ctx, frequency, 0.18, type, volume), t * 1000)
  }
}

export function usePomodoroSound() {
  let initialized = false

  function init() {
    if (initialized) return
    ensureAudioContext().then((ctx) => {
      if (ctx) initialized = true
    })
  }

  async function playWorkComplete(beeps = 5, volume = 0.3) {
    const ctx = await ensureAudioContext()
    if (ctx) {
      playBeeps(ctx, 880, beeps, volume, 'square')
      return
    }
    // 降级：HTTP 环境下用 <audio> 播放生成的 WAV
    playFallbackBeeps(880, beeps)
  }

  async function playBreakComplete(beeps = 5, volume = 0.3) {
    const ctx = await ensureAudioContext()
    if (ctx) {
      playBeeps(ctx, 660, beeps, volume, 'triangle')
      return
    }
    // 降级：HTTP 环境下用 <audio> 播放生成的 WAV
    playFallbackBeeps(660, beeps)
  }

  return { init, playWorkComplete, playBreakComplete }
}
