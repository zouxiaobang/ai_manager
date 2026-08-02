import {
  applyShellDocumentClass,
  prepareMobileEntryUrl,
  preparePcEntryUrl,
  resolveAppShell,
} from './utils/deviceShell'

async function boot() {
  const shell = resolveAppShell()
  applyShellDocumentClass(shell)

  if (shell === 'mobile') {
    prepareMobileEntryUrl()
    await import('./main_mobile')
  } else {
    preparePcEntryUrl()
    await import('./main_pc')
  }
}

void boot()
