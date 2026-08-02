import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * Vitest 独立配置：与 vite.config.ts 隔离，避免 VitePWA / manualChunks / proxy 干扰测试。
 * 仅挂载 vue 插件并解析 @ 别名，与主构建保持一致。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
    include: ['src/**/*.{test,spec}.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/**/*.{ts,vue}'],
      exclude: [
        'src/main_*.ts',
        'src/bootstrap.ts',
        'src/router/**',
        'src/i18n/**',
        'src/vite-env.d.ts',
        '**/__tests__/**',
      ],
    },
  },
})
