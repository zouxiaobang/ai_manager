export interface NoteTocItem {
  id: string
  level: number
  text: string
  index: number
}

export interface NoteTocTreeNode {
  id: string
  label: string
  level: number
  index: number
  children?: NoteTocTreeNode[]
}

const HEADING_SELECTOR = 'h1,h2,h3,h4,h5,h6'

export function parseNoteToc(html: string): NoteTocItem[] {
  if (!html?.trim()) return []
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const headings = doc.querySelectorAll(HEADING_SELECTOR)
  const items: NoteTocItem[] = []
  headings.forEach((el, index) => {
    const level = Number.parseInt(el.tagName.slice(1), 10)
    const text = el.textContent?.trim() ?? ''
    if (!text) return
    items.push({
      id: `nb-heading-${index}`,
      level,
      text,
      index,
    })
  })
  return items
}

export function buildNoteTocTree(items: NoteTocItem[]): NoteTocTreeNode[] {
  const roots: NoteTocTreeNode[] = []
  const stack: NoteTocTreeNode[] = []

  for (const item of items) {
    const node: NoteTocTreeNode = {
      id: item.id,
      label: item.text,
      level: item.level,
      index: item.index,
      children: [],
    }

    while (stack.length > 0 && stack[stack.length - 1].level >= item.level) {
      stack.pop()
    }

    if (stack.length === 0) {
      roots.push(node)
    } else {
      const parent = stack[stack.length - 1]
      parent.children!.push(node)
    }

    stack.push(node)
  }

  const prune = (nodes: NoteTocTreeNode[]) => {
    for (const node of nodes) {
      if (node.children?.length) {
        prune(node.children)
      } else {
        delete node.children
      }
    }
  }
  prune(roots)
  return roots
}

export { HEADING_SELECTOR }
