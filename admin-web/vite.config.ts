import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'
import { fileURLToPath, URL } from 'node:url'
import { resolve } from 'node:path'

const piBuild = process.env.PI_BUILD === '1'

// 代理目标可用 VITE_API_TARGET 覆盖（worktree 独立端口联调，见仓库根 dev.ps1）
const apiTarget = process.env.VITE_API_TARGET || 'http://127.0.0.1:8080'

function buildManualChunks(id: string) {
  if (!id.includes('node_modules')) {
    if (id.includes('MonthlySettlementPanel')) return 'monthly-settlement'
    if (id.includes('SalesOrderPanel')) return 'ec-sales-order'
    if (id.includes('NoteRichEditor')) return 'note-rich-editor'
    if (id.includes('DeployCenterView')) return 'deploy-center'
    if (id.includes('NotebookView')) return 'notebook'
    if (id.includes('StorageCenterView')) return 'storage-center'
    if (id.includes('EcommerceSettingsView')) return 'ec-settings'
    if (id.includes('pomodoro/ReportPanel')) return 'pomo-report'
    return undefined
  }
  if (id.includes('xterm')) return 'xterm'
  if (id.includes('echarts')) return 'echarts'
  if (id.includes('exceljs')) return 'exceljs'
  if (id.includes('three')) return 'three'
  if (id.includes('@wangeditor')) return 'wangeditor'
  if (id.includes('element-plus')) return 'element-plus'
  if (id.includes('jspdf') || id.includes('html2canvas')) return 'export-pdf'
  return undefined
}

export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: null,
      manifest: false,
      includeAssets: ['pwa/**/*'],
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,webmanifest,wasm}'],
        globIgnores: [
          '**/MonthlySettlementPanel*',
          '**/DeployCenterView*',
          '**/image-space/**',
          '**/preview/**',
          '**/*-scheme-*.png',
          '**/AlibabaPuHuiTi*.ttf',
        ],
        maximumFileSizeToCacheInBytes: 3 * 1024 * 1024,
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api/, /^\/uploads/],
      },
      disable: command === 'serve',
    }),
  ],
  build: {
    sourcemap: false,
    reportCompressedSize: !piBuild,
    rollupOptions: {
      maxParallelFileOps: piBuild ? 1 : undefined,
      input: {
        main: resolve(__dirname, 'index.html'),
        pc: resolve(__dirname, 'index_pc.html'),
        mobile: resolve(__dirname, 'mobile.html'),
      },
      output: {
        manualChunks(id) {
          return buildManualChunks(id)
        },
      },
    },
  },
  // element-plus 2.14+ 的 .mjs.map 含 VLQ 空字节，esbuild 预构建时会当 JS 解析报错
  optimizeDeps: {
    include: ['exceljs'],
    esbuildOptions: {
      sourcemap: false,
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 允许局域网设备通过本机 IP（如 192.168.x.x:5173）访问
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
      '/uploads': {
        target: apiTarget,
        changeOrigin: true,
      },
      '/claude-relay': {
        target: 'http://127.0.0.1:3001',
        ws: true,
        changeOrigin: true,
      },
    },
  },
}))
