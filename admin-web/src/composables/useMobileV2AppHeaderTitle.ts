import { ref } from 'vue'

// 让 mobile-v2 子页面把动态标题传给顶部 header
// （如笔记阅读页把 note.title 作为 header 标题，避免与卡片内标题重复占两行）
const dynamicTitle = ref<string | null>(null)

export function useMobileV2AppHeaderTitle() {
  function setHeaderTitle(title: string | null) {
    dynamicTitle.value = title
  }

  return { dynamicTitle, setHeaderTitle }
}
