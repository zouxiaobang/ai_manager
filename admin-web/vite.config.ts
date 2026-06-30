import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { resolve } from 'node:path'

const piBuild = process.env.PI_BUILD === '1'

function piManualChunks(id: string) {
  if (!id.includes('node_modules')) {
    if (id.includes('MonthlySettlementPanel')) return 'monthly-settlement'
    if (id.includes('NoteRichEditor')) return 'note-rich-editor'
    if (id.includes('DeployCenterView')) return 'deploy-center'
    return undefined
  }
  if (id.includes('echarts')) return 'echarts'
  if (id.includes('exceljs')) return 'exceljs'
  if (id.includes('three')) return 'three'
  if (id.includes('@wangeditor')) return 'wangeditor'
  if (id.includes('element-plus')) return 'element-plus'
  if (id.includes('jspdf') || id.includes('html2canvas')) return 'export-pdf'
  return undefined
}

export default defineConfig({
  plugins: [vue()],
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
      output: piBuild
        ? {
            manualChunks(id) {
              return piManualChunks(id)
            },
          }
        : undefined,
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
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
