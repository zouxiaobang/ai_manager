import { createApp, nextTick } from 'vue'
import CartonBox3DPreview from '@/components/ecommerce/CartonBox3DPreview.vue'
import i18n from '@/i18n'
import { captureElementToPngBlob } from '@/utils/captureElementToPng'
import type { RenderCarton3DOptions } from '@/utils/renderCarton3DToPng'

/** 使用与编辑页一致的 CSS 3D 预览截图（WebGL 不可用时的后备方案） */
export async function renderCartonCssToPngBlob(options: RenderCarton3DOptions): Promise<Blob | null> {
  const lengthCm = Number(options.lengthCm)
  const widthCm = Number(options.widthCm)
  const heightCm = Number(options.heightCm)
  if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) return null

  const container = document.createElement('div')
  container.style.cssText =
    'position:fixed;left:-12000px;top:0;width:520px;height:360px;overflow:hidden;pointer-events:none;opacity:0;'
  document.body.appendChild(container)

  const app = createApp(CartonBox3DPreview, {
    lengthCm,
    widthCm,
    heightCm,
    illustrationVariant: options.illustrationVariant,
    seed: options.seed,
    exportMode: true,
  })
  app.use(i18n)

  try {
    app.mount(container)
    await nextTick()
    if (document.fonts?.ready) {
      await document.fonts.ready
    }
    await new Promise<void>((resolve) => {
      requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
    })

    const scene = container.querySelector('.carton-box-3d__scene') as HTMLElement | null
    if (!scene) {
      throw new Error('预览场景未就绪')
    }

    return await captureElementToPngBlob(scene, {
      backgroundColor: options.backgroundColor ?? '#ffffff',
      scale: 2,
    })
  } finally {
    app.unmount()
    container.remove()
  }
}
