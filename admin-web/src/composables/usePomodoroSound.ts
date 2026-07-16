let audioCtx: AudioContext | null = null

function ensureAudioContext(): AudioContext | null {
  if (audioCtx) {
    if (audioCtx.state === 'suspended') {
      audioCtx.resume()
    }
    return audioCtx
  }
  try {
    audioCtx = new AudioContext()
    return audioCtx
  } catch {
    return null
  }
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
    const ctx = ensureAudioContext()
    if (ctx) {
      initialized = true
    }
  }

  function playWorkComplete(beeps = 5, volume = 0.3) {
    const ctx = ensureAudioContext()
    if (!ctx) return
    playBeeps(ctx, 880, beeps, volume, 'square')
  }

  function playBreakComplete(beeps = 5, volume = 0.3) {
    const ctx = ensureAudioContext()
    if (!ctx) return
    playBeeps(ctx, 660, beeps, volume, 'triangle')
  }

  return { init, playWorkComplete, playBreakComplete }
}
