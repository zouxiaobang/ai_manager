import type { Ref } from 'vue'

/**
 * 引用计数 loading：多个并发请求共享同一 loading 标志，
 * 全部结束才关闭。silent 调用不参与计数（后台静默刷新等场景）。
 */
export function useCountingLoading(active: Ref<boolean>) {
  let count = 0

  /** 进入一次加载：非 silent 时计数并点亮 loading */
  function begin(silent?: boolean) {
    if (silent) return
    count += 1
    active.value = true
  }

  /** 结束一次加载：计数归零才熄灭 loading，防御负计数 */
  function end(silent?: boolean) {
    if (silent) return
    count = Math.max(0, count - 1)
    if (count === 0) {
      active.value = false
    }
  }

  /** 重置计数与 loading（如清理旧请求遗留状态） */
  function reset() {
    count = 0
    active.value = false
  }

  return { begin, end, reset }
}
