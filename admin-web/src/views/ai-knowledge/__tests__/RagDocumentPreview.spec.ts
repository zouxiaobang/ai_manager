import { mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import RagDocumentPreview from '../RagDocumentPreview.vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

let wrapper: VueWrapper | undefined

function mountPreview(props: Partial<InstanceType<typeof RagDocumentPreview>['$props']> = {}) {
  wrapper = mount(RagDocumentPreview, {
    props: {
      visible: true,
      title: 'a.md',
      fileType: 'md',
      content: '',
      loading: false,
      ...props,
    },
    global: { plugins: [ElementPlus] },
    attachTo: document.body,
  })
  return wrapper
}

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  // 清空 Teleport 到 body 的残留 DOM，避免用例间互相污染
  document.body.innerHTML = ''
})

describe('RagDocumentPreview 组件渲染', () => {
  it('md 走 marked 渲染并复用 note-content-body 排版', async () => {
    mountPreview({ fileType: 'md', content: '# 标题\n\n**加粗**' })
    await nextTick()

    const md = document.body.querySelector('.note-content-body') as HTMLElement | null
    expect(md).toBeTruthy()
    expect(md!.innerHTML).toContain('<h1')
    expect(md!.innerHTML).toContain('<strong>加粗</strong>')
  })

  it('非 md 类型渲染为保留换行的纯文本', async () => {
    mountPreview({ fileType: 'pdf', content: 'PDF 正文\n第二行' })
    await nextTick()

    const pre = document.body.querySelector('.rag-doc-preview__plain') as HTMLElement | null
    expect(pre).toBeTruthy()
    expect(pre!.textContent).toContain('PDF 正文')
    expect(pre!.textContent).toContain('第二行')
    // 纯文本走 pre 保留换行，不经过 marked
    expect(document.body.querySelector('.note-content-body')).toBeNull()
  })

  it('loading 时不渲染正文内容', async () => {
    mountPreview({ fileType: 'md', content: '# x', loading: true })
    await nextTick()

    expect(document.body.querySelector('.note-content-body')).toBeNull()
    expect(document.body.querySelector('.rag-doc-preview__plain')).toBeNull()
  })

  it('空内容显示占位', async () => {
    mountPreview({ fileType: 'md', content: '', loading: false })
    await nextTick()

    expect(document.body.querySelector('.el-empty')).toBeTruthy()
  })

  it('visible 为 false 时不渲染面板', async () => {
    mountPreview({ visible: false })
    await nextTick()

    expect(document.body.querySelector('.rag-doc-preview__panel')).toBeNull()
  })

  it('点击关闭按钮触发 update:visible false', async () => {
    const wrap = mountPreview({ fileType: 'md', content: 'x' })
    await nextTick()

    const btn = document.body.querySelector('.rag-doc-preview__close-btn') as HTMLElement
    btn.click()

    expect(wrap.emitted('update:visible')?.[0]).toEqual([false])
  })
})
