<template>
  <div ref="rootRef" class="note-rich-editor">
    <div
      class="note-rich-editor__toolbar-wrap"
      @mousedown="onToolbarMouseDown"
      @click="schedulePatchDropPanel"
      @mouseover="schedulePatchDropPanel"
    >
      <Toolbar
        class="note-rich-editor__toolbar"
        :editor="editorRef"
        :default-config="toolbarConfig"
        mode="default"
      />
    </div>
    <Editor
      class="note-rich-editor__body"
      :model-value="html"
      :default-config="editorConfig"
      mode="default"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { computed, nextTick, onBeforeUnmount, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { i18nAddResources, i18nChangeLanguage } from '@wangeditor/editor'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { getNotebookImageUrl, uploadNotebookImage } from '@/api/notebook/image'
import { HEADING_SELECTOR } from './noteToc'

const { t, locale } = useI18n()

const insertLinkLabel = computed(() => t('notebook.insertLink'))

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
}>()

const editorRef = shallowRef<IDomEditor>()
const rootRef = shallowRef<HTMLElement | null>(null)
const html = shallowRef(normalizeHtmlContent(props.modelValue))

let dropPanelObserver: MutationObserver | null = null
let patchTimer: ReturnType<typeof setTimeout> | null = null
let suppressChange = false
let suppressChangeTimer: ReturnType<typeof setTimeout> | null = null

function hasVisibleHtml(html: string): boolean {
  const el = document.createElement('div')
  el.innerHTML = html
  return (el.textContent?.replace(/\u200B/g, '').trim().length ?? 0) > 0
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

function setupDropPanelObserver() {
  const root = rootRef.value
  if (!root || dropPanelObserver) return
  dropPanelObserver = new MutationObserver(() => schedulePatchDropPanel())
  dropPanelObserver.observe(root, { childList: true, subtree: true })
}

function schedulePatchDropPanel() {
  if (patchTimer) clearTimeout(patchTimer)
  patchTimer = setTimeout(() => {
    patchDropPanelMenus()
    patchToolbarPointer()
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
}

watch(
  () => props.modelValue,
  (value) => {
    const next = normalizeHtmlContent(value)
    if (next === html.value) return
    html.value = next
    if (editorRef.value && editorRef.value.getHtml() !== next) {
      runWithSuppressedChange(() => {
        editorRef.value?.setHtml(next)
      })
    }
  },
)

const toolbarConfig: Partial<IToolbarConfig> = {
  toolbarKeys: [
    'undo',
    'redo',
    '|',
    'clearStyle',
    '|',
    'headerSelect',
    'fontFamily',
    'fontSize',
    '|',
    'bold',
    'italic',
    'underline',
    'through',
    '|',
    'color',
    '|',
    'insertLink',
    '|',
    'bulletedList',
    'numberedList',
    'todo',
    '|',
    'justifyLeft',
    'justifyCenter',
    'justifyRight',
    'justifyJustify',
    '|',
    {
      key: 'group-insert',
      title: '插入',
      menuKeys: [
        'insertImage',
        'insertTable',
        'insertLink',
        'codeBlock',
        'divider',
        'blockquote',
      ],
    },
  ],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder ?? '开始记录…',
  MENU_CONF: {
    fontSize: {
      fontSizeList: ['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px', '36px'],
    },
    uploadImage: {
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
  editor.setHtml(html.value)
  editor.on('modalOrPanelShow', schedulePatchDropPanel)
  editor.on('focus', patchToolbarPointer)
  editor.on('change', patchToolbarPointer)
  await nextTick()
  disableEditorSpellcheck()
  setupDropPanelObserver()
  setTimeout(() => {
    setupDropPanelObserver()
    patchToolbarPointer()
    disableEditorSpellcheck()
  }, 300)
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

function patchDropPanelMenus() {
  requestAnimationFrame(() => {
    const root = rootRef.value
    if (!root) return
    root.querySelectorAll('.w-e-bar-item-menus-container').forEach((panel) => {
      panel.classList.add('nb-insert-menus')
      panel.querySelectorAll('.w-e-bar-item').forEach((item) => {
        const barItem = item as HTMLElement
        barItem.style.display = 'block'
        barItem.style.width = '100%'
        barItem.style.height = 'auto'
        barItem.style.padding = '0'
        barItem.style.textAlign = 'left'
      })
      panel.querySelectorAll('.w-e-bar-item button').forEach((button) => {
        const btn = button as HTMLButtonElement
        btn.style.display = 'flex'
        btn.style.alignItems = 'center'
        btn.style.justifyContent = 'flex-start'
        btn.style.width = '100%'
        btn.style.height = '36px'
        btn.style.padding = '0 16px'
        btn.style.gap = '8px'
        btn.removeAttribute('data-tooltip')
        btn.classList.remove('w-e-menu-tooltip-v5')
        if (btn.querySelector('.title') || btn.dataset.nbLinkPatched === '1') return
        const title = document.createElement('span')
        title.className = 'title'
        title.textContent = insertLinkLabel.value
        btn.appendChild(title)
        btn.dataset.nbLinkPatched = '1'
      })
    })
  })
}

function handleChange(editor: IDomEditor) {
  if (suppressChange) return
  const value = editor.getHtml()
  if (!hasVisibleHtml(value) && hasVisibleHtml(html.value)) {
    runWithSuppressedChange(() => {
      editor.setHtml(html.value)
    })
    return
  }
  html.value = value
  emit('update:modelValue', value)
  emit('change')
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
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
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

  const applyToEditor = () => {
    if (!editorRef.value) return
    if (editorRef.value.getHtml() !== next) {
      editorRef.value.setHtml(next)
    }
  }

  runWithSuppressedChange(applyToEditor)

  await nextTick()
  await new Promise<void>((resolve) => {
    window.setTimeout(resolve, 32)
  })

  const actual = editorRef.value?.getHtml() ?? ''
  if (hasVisibleHtml(next) && !hasVisibleHtml(actual)) {
    runWithSuppressedChange(applyToEditor, 160)
  }
}

defineExpose({ scrollToHeading, getHtml, setHtml })

onBeforeUnmount(() => {
  if (patchTimer) clearTimeout(patchTimer)
  if (suppressChangeTimer) clearTimeout(suppressChangeTimer)
  dropPanelObserver?.disconnect()
  dropPanelObserver = null
  editorRef.value?.destroy()
})
</script>

<style scoped lang="scss">
.note-rich-editor {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
}

.note-rich-editor__toolbar-wrap {
  flex-shrink: 0;
}

.note-rich-editor__toolbar {
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.note-rich-editor__body {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

:deep(.note-rich-editor__body > div) {
  height: auto !important;
  min-height: 100% !important;
  overflow: visible !important;
}

:deep(.w-e-text-container) {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  font-size: 14px;
  height: auto !important;
  min-height: 100% !important;
  overflow: visible !important;
}

:deep(.w-e-scroll) {
  height: auto !important;
  min-height: 100% !important;
  overflow: visible !important;
}

:deep(.w-e-text-container p),
:deep(.w-e-text-container li) {
  font-size: 14px;
}

:deep(.w-e-toolbar) {
  background: var(--el-fill-color-blank);
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: default;
}

:deep(.w-e-toolbar .w-e-bar-item) {
  cursor: pointer;
}

:deep(.w-e-bar-item button) {
  color: var(--el-text-color-regular);
  cursor: pointer !important;
}

:deep(.w-e-bar-item button.disabled) {
  cursor: pointer !important;
}

:deep(.w-e-toolbar .select-button) {
  cursor: pointer !important;
}
</style>

<!-- wangEditor「插入」菜单使用 bar-item-menus-container，insertLink 仅渲染图标 -->
<style lang="scss">
.note-rich-editor .nb-insert-menus,
.note-rich-editor .w-e-bar-item-menus-container {
  min-width: 168px;
  padding: 6px 0 !important;
}

.note-rich-editor .nb-insert-menus .w-e-bar-item,
.note-rich-editor .w-e-bar-item-menus-container .w-e-bar-item {
  display: block !important;
  width: 100% !important;
  height: auto !important;
  padding: 0 !important;
  text-align: left !important;
  cursor: pointer !important;
}

.note-rich-editor .nb-insert-menus .w-e-bar-item button,
.note-rich-editor .w-e-bar-item-menus-container .w-e-bar-item button {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-start !important;
  width: 100% !important;
  height: 36px !important;
  padding: 0 16px !important;
  gap: 8px;
  cursor: pointer !important;
}

.note-rich-editor .nb-insert-menus .w-e-bar-item button.disabled,
.note-rich-editor .w-e-bar-item-menus-container .w-e-bar-item button.disabled {
  cursor: pointer !important;
}

.note-rich-editor .nb-insert-menus .w-e-bar-item button .title,
.note-rich-editor .w-e-bar-item-menus-container .w-e-bar-item button .title {
  display: inline-block !important;
  margin-left: 8px;
  font-size: 14px;
}

.note-rich-editor .nb-insert-menus .w-e-bar-item button::before,
.note-rich-editor .nb-insert-menus .w-e-bar-item button::after,
.note-rich-editor .w-e-bar-item-menus-container .w-e-bar-item button::before,
.note-rich-editor .w-e-bar-item-menus-container .w-e-bar-item button::after {
  display: none !important;
  content: none !important;
  visibility: hidden !important;
  opacity: 0 !important;
}
</style>
