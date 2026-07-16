import {
  applyShellDocumentClass,
  prepareMobileEntryUrl,
  preparePcEntryUrl,
  resolveAppShell,
} from './utils/deviceShell'

function resolveMobileUIVersion(): 'v1' | 'v2' {
  if (typeof localStorage === 'undefined') return 'v2'
  const saved = localStorage.getItem('mobile-ui-version')
  if (saved === 'v1' || saved === 'v2') return saved
  localStorage.setItem('mobile-ui-version', 'v2')
  return 'v2'
}

async function boot() {
  const shell = resolveAppShell()
  applyShellDocumentClass(shell)

  if (shell === 'mobile') {
    prepareMobileEntryUrl()
    const uiVersion = resolveMobileUIVersion()
    if (uiVersion === 'v2') {
      await import('./main_mobileV2')
    } else {
      await import('./main_mobile')
    }
  } else {
    preparePcEntryUrl()
    await import('./main_pc')
  }
}

void boot()
