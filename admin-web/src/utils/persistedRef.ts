import { ref, watch, type Ref } from 'vue'

/**
 * 创建一个与 localStorage 双向同步的 ref。
 *
 * 创建时立即按 parse 读取持久化值（非法值由 parse 自行回退默认），
 * 之后每次 value 变化都会写回 localStorage。
 * 仅在前端环境（window 存在）生效，SSR 下退化为普通 ref。
 *
 * @param key          存储键
 * @param defaultValue 默认值
 * @param parse        将 localStorage 字符串解析为 T；需自行处理 null 与非法值并回退默认
 * @returns 与 localStorage 同步的 ref
 */
export function createPersistedRef<T>(
  key: string,
  defaultValue: T,
  parse: (raw: string | null) => T,
): Ref<T> {
  const data = ref(defaultValue) as Ref<T>
  if (typeof window !== 'undefined') {
    data.value = parse(window.localStorage.getItem(key))
    watch(data, (value) => {
      window.localStorage.setItem(key, String(value))
    })
  }
  return data
}
