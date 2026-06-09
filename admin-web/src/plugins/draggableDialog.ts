import { ElMessageBox } from 'element-plus'
import type { ElMessageBoxOptions } from 'element-plus'

type MessageBoxFn = typeof ElMessageBox.confirm

function isOptions(value: unknown): value is ElMessageBoxOptions {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function patchMessageBoxMethod(method: MessageBoxFn): MessageBoxFn {
  return ((message: string, title?: string | ElMessageBoxOptions, options?: ElMessageBoxOptions) => {
    if (isOptions(title)) {
      return method(message, { draggable: true, ...title })
    }
    return method(message, title, { draggable: true, ...(options ?? {}) })
  }) as MessageBoxFn
}

/** 为 ElMessageBox 确认/提示框启用标题栏拖动 */
export function setupDraggableMessageBox() {
  ElMessageBox.confirm = patchMessageBoxMethod(ElMessageBox.confirm)
  ElMessageBox.alert = patchMessageBoxMethod(ElMessageBox.alert)
  ElMessageBox.prompt = patchMessageBoxMethod(ElMessageBox.prompt)
}

/** el-config-provider 使用的 Dialog 全局配置 */
export const globalDialogConfig = {
  draggable: true,
} as const
