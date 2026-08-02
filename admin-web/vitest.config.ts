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
      // 覆盖率门禁分阶段放开：先聚焦已补测试的纯逻辑目录，逐步扩展
      include: [
        'src/utils/**',
        'src/composables/**',
        'src/stores/**',
        'src/api/**',
        'src/constants/**',
      ],
      exclude: [
        'src/main_*.ts',
        'src/bootstrap.ts',
        'src/router/**',
        'src/i18n/**',
        'src/vite-env.d.ts',
        '**/__tests__/**',
      ],
      // 初始门禁按「不回归」设当前覆盖水平以下，随测试补充逐步抬高至 80%
      thresholds: {
        lines: 8,
        functions: 30,
        branches: 40,
        statements: 8,
      },
    },
  },
})
