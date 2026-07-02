const INPUT_SELECTOR = 'input, textarea, select, [contenteditable="true"]'

function isIos(): boolean {
  return /iPhone|iPad|iPod/i.test(navigator.userAgent)
}

/** 缓解 iOS / PWA 在输入框聚焦后视口缩放无法恢复的问题 */
export function setupMobileInputViewportFix() {
  if (typeof document === 'undefined') return

  document.addEventListener(
    'focusout',
    (event) => {
      const target = event.target
      if (!(target instanceof HTMLElement) || !target.matches(INPUT_SELECTOR)) {
        return
      }

      window.setTimeout(() => {
        if (isIos()) {
          const meta = document.querySelector('meta[name="viewport"]')
          if (meta) {
            const content =
              'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover'
            meta.setAttribute('content', `${content}, maximum-scale=1.01`)
            meta.setAttribute('content', content)
          }
        }

        const main = document.querySelector('.mobile-app__main')
        if (main instanceof HTMLElement) {
          const { scrollTop } = main
          main.scrollTop = scrollTop
        }
      }, 120)
    },
    true,
  )
}
