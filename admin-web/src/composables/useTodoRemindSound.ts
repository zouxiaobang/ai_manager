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
  volume = 0.3,
) {
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = 'sine'
  osc.frequency.setValueAtTime(frequency, ctx.currentTime)
  gain.gain.setValueAtTime(volume, ctx.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration)
  osc.connect(gain)
  gain.connect(ctx.destination)
  osc.start(ctx.currentTime)
  osc.stop(ctx.currentTime + duration)
}

function playOneChime(ctx: AudioContext, volume = 0.3) {
  playNote(ctx, 523, 0.2, volume)
  setTimeout(() => playNote(ctx, 659, 0.25, volume), 150)
  setTimeout(() => playNote(ctx, 784, 0.35, volume), 300)
}

export function useTodoRemindSound() {
  let initialized = false

  function init() {
    if (initialized) return
    const ctx = ensureAudioContext()
    if (ctx) {
      initialized = true
    }
  }

  function play(volume = 0.3, beeps = 3) {
    const ctx = ensureAudioContext()
    if (!ctx) return
    for (let i = 0; i < beeps; i++) {
      setTimeout(() => playOneChime(ctx, volume), i * 800)
    }
  }

  return { init, play }
}
