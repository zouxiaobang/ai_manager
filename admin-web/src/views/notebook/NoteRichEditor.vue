<template>
  <div ref="rootRef" class="note-rich-editor">
    <div
      ref="toolbarRef"
      class="note-rich-editor__toolbar-wrap"
      @mousedown="onToolbarMouseDown"
      @click="schedulePatchDropPanel"
      @mouseover="schedulePatchDropPanel"
    />
    <Editor
      class="note-rich-editor__body"
      :default-html="initialEditorHtml"
      :default-config="editorConfig"
      mode="default"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
    <StorageImagePickerDialog
      v-model="imagePickerVisible"
      scope="notebook"
      :upload-file="uploadNotebookImage"
      @confirm="onNotebookImagePicked"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { nextTick, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Editor } from '@wangeditor/editor-for-vue'
import { createToolbar, DomEditor, i18nAddResources, i18nChangeLanguage } from '@wangeditor/editor'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { getNotebookImageUrl, uploadNotebookImage } from '@/api/notebook/image'
import StorageImagePickerDialog from '@/components/storage/StorageImagePickerDialog.vue'
import { HEADING_SELECTOR } from './noteToc'
import { hasNoteVisibleText } from './noteContentOptimize'
import { APP_FONT_FAMILY_VALUE } from '@/constants/font-family'

const { locale, t } = useI18n()

const MORE_TOOLS_ICON_SVG =
  '<svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path d="M192 512m64 0a64 64 0 1 0-128 0 64 64 0 1 0 128 0Zm320 0a64 64 0 1 0-128 0 64 64 0 1 0 128 0Zm320 0a64 64 0 1 0-128 0 64 64 0 1 0 128 0Z"></path></svg>'

function buildToolbarConfig(): Partial<IToolbarConfig> {
  return {
    toolbarKeys: [
      'undo',
      'redo',
      '|',
      'headerSelect',
      '|',
      'bold',
      'italic',
      'through',
      'code',
      'insertLink',
      '|',
      'bulletedList',
      'numberedList',
      'todo',
      '|',
      'codeBlock',
      'blockquote',
      '|',
          'insertTable',
          'insertImage',
          '|',
      {
        key: 'group-more',
        title: t('notebook.moreTools'),
        iconSvg: MORE_TOOLS_ICON_SVG,
        menuKeys: [
          'clearStyle',
          'fontFamily',
          'fontSize',
          'underline',
          'color',
          'justifyLeft',
          'justifyCenter',
          'justifyRight',
          'justifyJustify',
          'divider',
        ],
      },
      '|',
      'fullScreen',
    ],
  }
}

let scrollRoot: HTMLElement | null = null
let scrollRaf: number | null = null
let onWindowReflow: (() => void) | null = null
let patchTimer: ReturnType<typeof setTimeout> | null = null

watch(
  locale,
  (value) => {
    const editorLocale = value === 'en-US' ? 'en' : 'zh-CN'
    i18nAddResources(editorLocale, {
      fontSize: {
        default: '14px',
      },
      link: {
        insert: value === 'en-US' ? 'Link' : '链接',
      },
    })
    i18nChangeLanguage(editorLocale)
    schedulePatchDropPanel()
  },
  { immediate: true },
)

const props = defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  change: []
  'heading-active': [index: number]
}>()

const editorRef = shallowRef<IDomEditor>()
const rootRef = shallowRef<HTMLElement | null>(null)
const toolbarRef = shallowRef<HTMLElement | null>(null)
const initialEditorHtml = normalizeHtmlContent(props.modelValue)
const html = shallowRef(initialEditorHtml)

let dropPanelObserver: MutationObserver | null = null
let suppressChange = false
let suppressChangeTimer: ReturnType<typeof setTimeout> | null = null
let toolbarTooltipEl: HTMLElement | null = null
let toolbarTooltipAnchor: HTMLElement | null = null

function formatTooltipText(raw: string): string {
  const parts = raw
    .split('\n')
    .map((part) => part.trim())
    .filter(Boolean)
  if (!parts.length) return ''
  if (parts.length === 1) return parts[0]
  return `${parts[0]} (${parts.slice(1).join(', ')})`
}

function getToolbarButtonTip(button: HTMLElement): string {
  const dataTip = button.getAttribute('data-tooltip')?.trim()
  if (dataTip) return formatTooltipText(dataTip)
  const titleText = button.querySelector('.title')?.textContent?.trim()
  if (titleText) return titleText
  return button.getAttribute('aria-label')?.trim() ?? ''
}

function ensureToolbarTooltipEl(): HTMLElement {
  if (!toolbarTooltipEl) {
    toolbarTooltipEl = document.createElement('div')
    toolbarTooltipEl.className = 'note-editor-toolbar-tooltip'
    toolbarTooltipEl.setAttribute('role', 'tooltip')
    document.body.appendChild(toolbarTooltipEl)
  }
  return toolbarTooltipEl
}

function positionToolbarTooltip(tip: HTMLElement, anchor: HTMLElement) {
  const rect = anchor.getBoundingClientRect()
  tip.style.display = 'block'
  tip.style.visibility = 'hidden'
  tip.style.left = '0'
  tip.style.top = '0'

  const tipRect = tip.getBoundingClientRect()
  let left = rect.left + rect.width / 2 - tipRect.width / 2
  left = Math.max(8, Math.min(left, window.innerWidth - tipRect.width - 8))

  let top = rect.bottom + 8
  if (top + tipRect.height > window.innerHeight - 8) {
    top = rect.top - tipRect.height - 8
  }

  tip.style.left = `${left}px`
  tip.style.top = `${top}px`
  tip.style.visibility = 'visible'
}

function showToolbarTooltip(event: Event) {
  const anchor = event.currentTarget as HTMLElement | null
  if (!anchor) return
  const text = getToolbarButtonTip(anchor)
  if (!text) return

  const tip = ensureToolbarTooltipEl()
  tip.textContent = text
  toolbarTooltipAnchor = anchor
  positionToolbarTooltip(tip, anchor)
}

function hideToolbarTooltip() {
  toolbarTooltipAnchor = null
  if (toolbarTooltipEl) toolbarTooltipEl.style.display = 'none'
}

function patchToolbarTooltips() {
  const root = rootRef.value
  if (!root) return

  const moreButton = root.querySelector<HTMLElement>('.w-e-toolbar button[data-menu-key="group-more"]')
  if (moreButton) {
    moreButton.setAttribute('data-tooltip', t('notebook.moreTools'))
  }

  const buttons = root.querySelectorAll<HTMLElement>(
    '.w-e-toolbar .w-e-bar-item > button, .w-e-toolbar .select-button, .w-e-bar-item-menus-container .w-e-bar-item > button',
  )

  buttons.forEach((button) => {
    if (button.dataset.noteTipBound === '1') return
    button.dataset.noteTipBound = '1'
    button.addEventListener('mouseenter', showToolbarTooltip)
    button.addEventListener('mouseleave', hideToolbarTooltip)
    button.addEventListener('mousedown', hideToolbarTooltip)
  })
}

function applyEditorHtml(next: string, suppressMs = 160) {
  const editor = editorRef.value
  if (!editor) return
  const normalized = normalizeHtmlContent(next)
  if (!normalized.trim()) return
  if (editor.getHtml() === normalized) return
  runWithSuppressedChange(() => {
    editor.setHtml(normalized)
  }, suppressMs)
}

function runWithSuppressedChange(run: () => void, releaseMs = 120) {
  suppressChange = true
  if (suppressChangeTimer) clearTimeout(suppressChangeTimer)
  run()
  void nextTick(() => {
    suppressChangeTimer = setTimeout(() => {
      suppressChange = false
      suppressChangeTimer = null
    }, releaseMs)
  })
}

function bindFloatingPanelReflow() {
  if (onWindowReflow) return
  onWindowReflow = () => schedulePatchDropPanel()
  window.addEventListener('scroll', onWindowReflow, true)
  window.addEventListener('resize', onWindowReflow)
}

function setupDropPanelObserver() {
  const root = rootRef.value
  if (!root || dropPanelObserver) return
  dropPanelObserver = new MutationObserver(() => schedulePatchDropPanel())
  dropPanelObserver.observe(root, { childList: true, subtree: true })
  bindFloatingPanelReflow()
}

function schedulePatchDropPanel() {
  if (patchTimer) clearTimeout(patchTimer)
  patchTimer = setTimeout(() => {
    patchDropPanelMenus()
    patchToolbarPointer()
    patchToolbarTooltips()
    if (toolbarTooltipAnchor && toolbarTooltipEl?.style.display === 'block') {
      positionToolbarTooltip(toolbarTooltipEl, toolbarTooltipAnchor)
    }
  }, 0)
}

function onToolbarMouseDown() {
  editorRef.value?.focus()
  schedulePatchDropPanel()
}

function patchToolbarPointer() {
  const root = rootRef.value
  if (!root) return
  disableEditorSpellcheck()
  root.querySelectorAll('.w-e-toolbar button, .w-e-bar-item-menus-container button').forEach((button) => {
    const btn = button as HTMLButtonElement
    btn.style.cursor = 'pointer'
  })
  root.querySelectorAll('.w-e-toolbar .w-e-bar-item').forEach((item) => {
    item.classList.remove('is-fullscreen', 'is-more-tools')
  })
  root.querySelector('.w-e-toolbar button[data-menu-key="group-more"]')
    ?.closest('.w-e-bar-item')
    ?.classList.add('is-more-tools')
  root.querySelector('.w-e-toolbar button[data-menu-key="fullScreen"]')
    ?.closest('.w-e-bar-item')
    ?.classList.add('is-fullscreen')
}

function initToolbar(editor: IDomEditor) {
  const container = toolbarRef.value
  if (!container || DomEditor.getToolbar(editor)) return
  try {
    createToolbar({
      editor,
      selector: container,
      config: buildToolbarConfig(),
      mode: 'default',
    })
    schedulePatchDropPanel()
  } catch (error) {
    console.error('[NoteRichEditor] toolbar init failed', error)
  }
}

async function ensureToolbar(editor: IDomEditor) {
  await nextTick()
  initToolbar(editor)
  if (!DomEditor.getToolbar(editor)) {
    window.setTimeout(() => initToolbar(editor), 0)
  }
  patchToolbarPointer()
}

const imagePickerVisible = ref(false)
let pendingImageInsert: ((url: string, alt: string, href: string) => void) | null = null

watch(
  () => props.modelValue,
  (value) => {
    const next = normalizeHtmlContent(value)
    if (next === html.value) return
    html.value = next
    applyEditorHtml(next)
  },
)

watch(imagePickerVisible, (open) => {
  if (!open) {
    pendingImageInsert = null
  }
})

function onNotebookImagePicked(fileName: string) {
  const url = getNotebookImageUrl(fileName)
  pendingImageInsert?.(url, fileName, url)
  pendingImageInsert = null
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder ?? '开始记录…',
  readOnly: false,
  autoFocus: true,
  MENU_CONF: {
    fontFamily: {
      fontFamilyList: [{ name: '阿里巴巴普惠体', value: APP_FONT_FAMILY_VALUE }],
    },
    fontSize: {
      fontSizeList: ['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px', '36px'],
    },
    uploadImage: {
      customBrowseAndUpload(
        insertFn: (url: string, alt: string, href: string) => void,
      ) {
        pendingImageInsert = insertFn
        imagePickerVisible.value = true
      },
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        const fileName = await uploadNotebookImage(file)
        const url = getNotebookImageUrl(fileName)
        insertFn(url, file.name, url)
      },
    },
  },
}

function normalizeHtmlContent(content?: string | null): string {
  const text = content ?? ''
  if (!text.trim()) return ''
  if (/<[a-z][\s\S]*>/i.test(text)) return text
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped
    .split('\n')
    .map((line) => (line ? `<p>${line}</p>` : '<p><br></p>'))
    .join('')
}

async function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
  editor.on('modalOrPanelShow', schedulePatchDropPanel)
  editor.on('modalOrPanelHide', schedulePatchDropPanel)
  editor.on('focus', patchToolbarPointer)
  editor.on('change', patchToolbarPointer)
  await ensureToolbar(editor)
  runWithSuppressedChange(() => {
    editor.setHtml(html.value)
  })
  disableEditorSpellcheck()
  setupDropPanelObserver()
  setupScrollSpy()
  window.setTimeout(() => {
    void ensureToolbar(editor)
    setupDropPanelObserver()
    setupScrollSpy()
    patchToolbarPointer()
    disableEditorSpellcheck()
    updateActiveHeading()
  }, 120)
}

function getScrollRoot(): HTMLElement | null {
  return (
    rootRef.value?.querySelector<HTMLElement>('.note-rich-editor__body .w-e-scroll') ??
    rootRef.value?.querySelector<HTMLElement>('.note-rich-editor__body') ??
    null
  )
}

function captureEditorScrollTop(): number {
  return getScrollRoot()?.scrollTop ?? 0
}

function restoreEditorScrollTop(scrollTop: number) {
  const root = getScrollRoot()
  if (!root) return
  const apply = () => {
    root.scrollTop = scrollTop
  }
  apply()
  requestAnimationFrame(apply)
}

function ensureSelectionVisible() {
  const root = getScrollRoot()
  const selection = window.getSelection()
  if (!root || !selection?.rangeCount) return

  const range = selection.getRangeAt(0)
  const rects = range.getClientRects()
  const rect = rects.length ? rects[rects.length - 1] : range.getBoundingClientRect()
  if (!rect.width && !rect.height) return

  const rootRect = root.getBoundingClientRect()
  const padding = 32

  if (rect.bottom > rootRect.bottom - padding) {
    root.scrollTop += rect.bottom - rootRect.bottom + padding
  } else if (rect.top < rootRect.top + padding) {
    root.scrollTop -= rootRect.top + padding - rect.top
  }
}

/** 仅在异常跳到顶部时恢复滚动；否则让视口跟随光标（如底部回车换行） */
function reconcileEditorScroll(savedScrollTop: number) {
  const root = getScrollRoot()
  if (!root) return

  const apply = () => {
    if (savedScrollTop > 60 && root.scrollTop < 20) {
      root.scrollTop = savedScrollTop
      return
    }
    ensureSelectionVisible()
  }

  requestAnimationFrame(() => {
    apply()
    requestAnimationFrame(apply)
  })
}

function isSelectionInCodeBlock(): boolean {
  const selection = window.getSelection()
  if (!selection?.anchorNode) return false
  const anchor =
    selection.anchorNode.nodeType === Node.TEXT_NODE
      ? selection.anchorNode.parentElement
      : (selection.anchorNode as Element)
  return !!anchor?.closest('pre, code')
}

function setupScrollSpy() {
  const nextRoot = getScrollRoot()
  if (scrollRoot === nextRoot) return
  scrollRoot?.removeEventListener('scroll', onEditorScroll)
  scrollRoot = nextRoot
  scrollRoot?.addEventListener('scroll', onEditorScroll, { passive: true })
}

function onEditorScroll() {
  if (scrollRaf != null) return
  scrollRaf = window.requestAnimationFrame(() => {
    scrollRaf = null
    updateActiveHeading()
  })
}

function updateActiveHeading() {
  const container = getEditableContainer()
  const root = getScrollRoot()
  if (!container || !root) return

  const headings = Array.from(container.querySelectorAll(HEADING_SELECTOR)) as HTMLElement[]
  if (!headings.length) {
    emit('heading-active', -1)
    return
  }

  const anchor = root.getBoundingClientRect().top + 72
  let activeIndex = 0
  for (let i = 0; i < headings.length; i++) {
    if (headings[i].getBoundingClientRect().top <= anchor) {
      activeIndex = i
    } else {
      break
    }
  }
  emit('heading-active', activeIndex)
}

function disableEditorSpellcheck() {
  const root = rootRef.value
  if (!root) return
  root.querySelectorAll('[contenteditable="true"]').forEach((el) => {
    el.setAttribute('spellcheck', 'false')
    el.setAttribute('autocorrect', 'off')
    el.setAttribute('autocapitalize', 'off')
  })
}

const FLOATING_PANEL_SELECTOR =
  '.w-e-bar-item-menus-container, .w-e-select-list, .w-e-drop-panel, .w-e-modal'

function isPanelVisible(panel: HTMLElement): boolean {
  const style = window.getComputedStyle(panel)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  return panel.getClientRects().length > 0
}

function resetFloatingPanel(panel: HTMLElement) {
  panel.style.position = ''
  panel.style.left = ''
  panel.style.top = ''
  panel.style.right = ''
  panel.style.bottom = ''
  panel.style.marginTop = ''
  panel.style.marginBottom = ''
  panel.style.minWidth = ''
  panel.style.zIndex = ''
  delete panel.dataset.floating
}

function patchDropPanelMenus() {
  requestAnimationFrame(() => {
    const root = rootRef.value
    if (!root) return

    root.querySelectorAll(FLOATING_PANEL_SELECTOR).forEach((node) => {
      const panel = node as HTMLElement
      if (!isPanelVisible(panel)) {
        if (panel.dataset.floating === '1') resetFloatingPanel(panel)
        return
      }

      const barItem = panel.closest('.w-e-bar-item')
      const anchor = (barItem?.querySelector('button') ?? barItem ?? panel.parentElement) as HTMLElement | null
      if (!anchor) return

      const rect = anchor.getBoundingClientRect()
      const useBottom = barItem?.closest('.w-e-bar-bottom') != null
      panel.style.position = 'fixed'
      panel.style.left = `${Math.max(8, rect.left)}px`
      panel.style.marginTop = '0'
      panel.style.marginBottom = '0'
      panel.style.zIndex = '4000'
      panel.dataset.floating = '1'

      if (useBottom) {
        panel.style.top = ''
        panel.style.bottom = `${Math.max(8, window.innerHeight - rect.top)}px`
      } else {
        panel.style.bottom = ''
        panel.style.top = `${rect.bottom + 4}px`
      }

      if (panel.classList.contains('w-e-select-list')) {
        panel.style.minWidth = `${rect.width}px`
      }

      panel.querySelectorAll('button').forEach((button) => {
        ;(button as HTMLButtonElement).style.cursor = 'pointer'
      })
    })
  })
}

function handleChange(editor: IDomEditor) {
  if (suppressChange) return
  const savedScrollTop = captureEditorScrollTop()
  const inCodeBlock = isSelectionInCodeBlock()
  const value = editor.getHtml()
  const fallbackHtml = html.value || props.modelValue
  if (!hasNoteVisibleText(value)) {
    if (hasNoteVisibleText(fallbackHtml)) {
      runWithSuppressedChange(() => {
        editor.setHtml(fallbackHtml)
      }, 200)
      restoreEditorScrollTop(savedScrollTop)
      return
    }
    reconcileEditorScroll(savedScrollTop)
    return
  }
  html.value = value
  emit('update:modelValue', value)
  emit('change')
  reconcileEditorScroll(savedScrollTop)
  if (!inCodeBlock) {
    void nextTick(() => updateActiveHeading())
  }
}

function getEditableContainer(): HTMLElement | null {
  const root = rootRef.value
  if (!root) return null
  return (
    root.querySelector<HTMLElement>('.w-e-text-container [contenteditable="true"]') ??
    root.querySelector<HTMLElement>('.w-e-text-container')
  )
}

function scrollToHeading(index: number) {
  const container = getEditableContainer()
  if (!container) return
  const headings = container.querySelectorAll(HEADING_SELECTOR)
  const target = headings[index] as HTMLElement | undefined
  if (!target) return
  const root = getScrollRoot()
  if (root) {
    const top = target.offsetTop - 12
    root.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
  } else {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  emit('heading-active', index)
  editorRef.value?.focus()
}

function getHtml(): string {
  return editorRef.value?.getHtml() ?? html.value ?? ''
}

async function setHtml(value: string) {
  const next = normalizeHtmlContent(value)
  if (!next.trim()) return

  html.value = next
  emit('update:modelValue', next)

  const applyToEditor = () => applyEditorHtml(next, 200)

  runWithSuppressedChange(applyToEditor, 200)

  await nextTick()
  await new Promise<void>((resolve) => {
    window.setTimeout(resolve, 48)
  })

  const actual = editorRef.value?.getHtml() ?? ''
  if (hasNoteVisibleText(next) && !hasNoteVisibleText(actual)) {
    runWithSuppressedChange(applyToEditor, 240)
  }
}

defineExpose({ scrollToHeading, getHtml, setHtml })

onBeforeUnmount(() => {
  if (patchTimer) clearTimeout(patchTimer)
  if (suppressChangeTimer) clearTimeout(suppressChangeTimer)
  if (scrollRaf != null) window.cancelAnimationFrame(scrollRaf)
  scrollRoot?.removeEventListener('scroll', onEditorScroll)
  scrollRoot = null
  dropPanelObserver?.disconnect()
  dropPanelObserver = null
  if (onWindowReflow) {
    window.removeEventListener('scroll', onWindowReflow, true)
    window.removeEventListener('resize', onWindowReflow)
    onWindowReflow = null
  }
  hideToolbarTooltip()
  toolbarTooltipEl?.remove()
  toolbarTooltipEl = null
  toolbarTooltipAnchor = null
  editorRef.value?.destroy()
})
</script>

<style scoped lang="scss">
.note-rich-editor {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  min-height: 300px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  overflow: visible;
  background: var(--wr-card, #fff);
  position: relative;
  z-index: 1;
  font-family: var(--app-font-family);
}

.note-rich-editor__toolbar-wrap {
  flex-shrink: 0;
  padding: 0 12px;
  background: transparent;
  overflow: visible;
  position: relative;
  z-index: 20;

  :deep(.w-e-toolbar) {
    flex-wrap: nowrap;
    width: max-content;
    min-width: 100%;
    overflow: visible;
  }
}

.note-rich-editor__body {
  flex: 1 1 auto;
  min-height: 260px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--wr-card, #fff);
  position: relative;
  z-index: 0;
  border-radius: 0 0 10px 10px;
}

:deep(.note-rich-editor__body > div) {
  flex: 1 1 auto;
  height: 100% !important;
  min-height: 0 !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
}

:deep(.w-e-text-container) {
  background: transparent;
  color: var(--wr-text, #333);
  font-size: 14px;
  line-height: 1.75;
  flex: 1 1 auto;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

:deep(.w-e-text-container [data-slate-editor]) {
  padding: 12px 20px 72px !important;
  min-height: 100%;
}

:deep(.w-e-scroll) {
  flex: 1 1 auto;
  height: 100% !important;
  min-height: 0 !important;
  overflow-y: auto !important;
}

:deep(.w-e-text-container p),
:deep(.w-e-text-container li) {
  font-size: 14px;
}

:deep(.w-e-text-placeholder) {
  top: 12px;
  left: 20px;
  font-style: normal;
  color: var(--wr-muted, #999);
}

:deep(.w-e-toolbar) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 2px;
  padding: 8px 0;
  --w-e-toolbar-bg-color: transparent;
  --w-e-toolbar-color: #595959;
  --w-e-toolbar-active-color: #333;
  --w-e-toolbar-active-bg-color: rgb(0 0 0 / 6%);
  --w-e-toolbar-disabled-color: #bfbfbf;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  cursor: default;
}

:deep(.w-e-text-container) {
  border: none !important;
  box-shadow: none !important;
  font-family: var(--app-font-family);
}

:deep(.w-e-toolbar .w-e-bar-item) {
  cursor: pointer;
  flex-shrink: 0;
}

:deep(.w-e-toolbar .w-e-bar-item.is-fullscreen) {
  margin-left: auto;
}

:deep(.w-e-toolbar .w-e-bar-item.is-more-tools button) {
  width: 28px;
  min-width: 28px;
  padding: 0;
}

:deep(.w-e-toolbar .w-e-bar-item) {
  overflow: visible;
}

:deep(.w-e-toolbar .w-e-bar-item button) {
  overflow: visible;
}

:deep(.w-e-toolbar button[data-menu-key='group-more'] svg:last-child) {
  display: none;
}

:deep(.w-e-bar-item-menus-container),
:deep(.w-e-select-list),
:deep(.w-e-drop-panel) {
  z-index: 4000 !important;
  background: var(--wr-card, #fff) !important;
  border: 1px solid var(--wr-border, #e8ecef) !important;
  box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 8%)) !important;
}

:deep(.w-e-bar-divider) {
  width: 1px;
  height: 16px;
  margin: 0 6px;
  background: var(--wr-border, #e8ecef);
  flex-shrink: 0;
}

:deep(.w-e-bar-item button) {
  cursor: pointer !important;
}

:deep(.w-e-bar-item button.disabled) {
  cursor: not-allowed !important;
}

:deep(.w-e-toolbar .select-button) {
  cursor: pointer !important;
}
</style>

<!-- wangEditor 下拉菜单：避免被父级裁切 -->
<style lang="scss">
.note-rich-editor .w-e-bar-item-menus-container {
  min-width: 168px;
  padding: 6px 0 !important;
}

.note-rich-editor .w-e-select-list {
  padding: 6px 0 !important;

  ul li {
    padding: 10px 20px !important;
    text-align: left;

    svg {
      left: 8px !important;
      margin-left: 0 !important;
    }
  }
}

.note-rich-editor .w-e-menu-tooltip-v5::before,
.note-rich-editor .w-e-menu-tooltip-v5::after {
  display: none !important;
}

.note-editor-toolbar-tooltip {
  position: fixed;
  z-index: 6000;
  display: none;
  max-width: 240px;
  padding: 5px 10px;
  border-radius: 6px;
  background: rgb(38 38 38 / 92%);
  color: #fff;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  pointer-events: none;
  box-shadow: 0 4px 12px rgb(0 0 0 / 12%);
}
</style>
