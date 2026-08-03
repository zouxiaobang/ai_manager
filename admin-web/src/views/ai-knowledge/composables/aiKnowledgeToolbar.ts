/** 消息类型（仅需 content 字段，避免依赖 ChatMessage 全量类型） */
export interface ToolbarMessage {
  content?: string | null
}

/**
 * 取工具栏命令对应消息的「上一条用户消息」标题：
 * 截取前 50 字符、折叠换行为空格，空内容回退占位「AI 问答」。
 * 供「导入笔记」「导出 PDF」两个命令共用，消除重复代码。
 */
export function deriveToolbarTitle(messages: ToolbarMessage[], msgIndex: number): string {
  const userMsg = msgIndex > 0 ? messages[msgIndex - 1] : null
  return userMsg?.content?.slice(0, 50).replace(/\n/g, ' ').trim() || 'AI 问答'
}

/**
 * 构建「AI 问答 → 导入笔记本」的 Markdown 内容：
 * 头部为引用样式的标题与日期，分隔线后为 AI 回答原文。
 */
export function buildImportNoteMarkdown(title: string, content: string, dateStr: string): string {
  return `> **${title}**  ·  📅 ${dateStr}

---

${content}`
}

/**
 * 递归查找树节点：先匹配当前节点，再逐层下钻 children。
 * 泛型约束 children 为可选子节点数组，兼容 NbTreeNode 等树形结构。
 */
export function findTreeNode<T extends { children?: T[] }>(nodes: T[], predicate: (n: T) => boolean): T | null {
  for (const node of nodes) {
    if (predicate(node)) return node
    if (node.children) {
      const found = findTreeNode(node.children, predicate)
      if (found) return found
    }
  }
  return null
}

/**
 * 工具栏命令纯函数库：供 AiKnowledgeView 的 handleToolbarCommand 使用，
 * 把「标题提取 / 笔记内容构建 / 树查找」从组件副作用中分离，便于单元测试。
 */
export function useAiKnowledgeToolbar() {
  return { deriveToolbarTitle, buildImportNoteMarkdown, findTreeNode }
}
