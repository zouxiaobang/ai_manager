/** 方案 A 涂鸦资源路径 */
const SCHEME_A_BASE = `${import.meta.env.BASE_URL}mobile-home/scheme-a`
/** 头图版本号：更新素材后递增，避免 PWA/浏览器强缓存 */
const HERO_ASSET_VER = '10'

function heroAsset(file: string) {
  return `${SCHEME_A_BASE}/${file}?v=${HERO_ASSET_VER}`
}

export const schemeAAssets = {
  mascotBear: `${SCHEME_A_BASE}/mascot-bear.svg`,
  heroTitleOverlay: heroAsset('hero-title-overlay.png'),
  heroBearOverlay: heroAsset('hero-bear-overlay.png'),
  starYellow: `${SCHEME_A_BASE}/deco-star-yellow.svg`,
  starBlue: `${SCHEME_A_BASE}/deco-star-blue.svg`,
  starBlueOutline: `${SCHEME_A_BASE}/deco-star-blue-outline.svg`,
  squiggleBlue: `${SCHEME_A_BASE}/deco-squiggle-blue.svg`,
  squiggleRed: `${SCHEME_A_BASE}/deco-squiggle-red.svg`,
  paperclip: `${SCHEME_A_BASE}/deco-paperclip.svg`,
  search: `${SCHEME_A_BASE}/icon-search.svg`,
  notebook: `${SCHEME_A_BASE}/icon-notebook.svg`,
  cart: `${SCHEME_A_BASE}/icon-cart.svg`,
  shield: `${SCHEME_A_BASE}/icon-shield.svg`,
  todos: `${SCHEME_A_BASE}/icon-todos.svg`,
  ecommerce: `${SCHEME_A_BASE}/icon-ecommerce.svg`,
  pixelDog: `${SCHEME_A_BASE}/icon-pixel-dog.svg`,
  tabHome: `${SCHEME_A_BASE}/tab-home.svg`,
  tabNotebook: `${SCHEME_A_BASE}/tab-notebook.svg`,
  tabTodos: `${SCHEME_A_BASE}/tab-todos.svg`,
  tabMore: `${SCHEME_A_BASE}/tab-more.svg`,
} as const

export const schemeAModuleIcons: Record<string, string> = {
  notebook: schemeAAssets.notebook,
  todos: schemeAAssets.todos,
  ecommerce: schemeAAssets.ecommerce,
  pixelDog: schemeAAssets.pixelDog,
  userCenter: schemeAAssets.notebook,
  permission: schemeAAssets.todos,
  storage: schemeAAssets.notebook,
  settings: schemeAAssets.todos,
}

export const schemeAModuleSquiggleColors: Record<string, string> = {
  notebook: '#2563eb',
  todos: '#e63946',
  ecommerce: '#f59e0b',
  pixelDog: '#16a34a',
  userCenter: '#6366f1',
  permission: '#0ea5e9',
  storage: '#6b7280',
  settings: '#9ca3af',
}

export const schemeAModuleBorderColors: Record<string, string> = {
  notebook: '#3b82f6',
  todos: '#ef4444',
  ecommerce: '#f59e0b',
  pixelDog: '#22c55e',
  userCenter: '#6366f1',
  permission: '#0ea5e9',
  storage: '#94a3b8',
  settings: '#9ca3af',
}
