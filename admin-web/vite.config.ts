import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { resolve } from 'node:path'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        pc: resolve(__dirname, 'index_pc.html'),
        mobile: resolve(__dirname, 'mobile.html'),
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
