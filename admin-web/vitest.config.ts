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
    // 仅处理 note-content.scss 的 ?inline 导入，让打印窗口内联的 CSS 在单测中真实可用；
    // 其余 CSS 仍按默认 stub，避免拖慢整体测试。
    css: { include: [/note-content\.scss\?inline/] },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      // 阶段 7：收敛到已测纯函数 / composable / store 文件（阶段 1/5/6 拆分并附测试的成果），
      // 对其开 80% 阈值防回归；其余文件随补测达标后再逐个放宽纳入 include。
      include: [
        'src/utils/{date,formatMoney,persistedRef,salesOrderView}.ts',
        'src/composables/{usePagination,useCountingLoading}.ts',
        'src/stores/{app,ecSettings}.ts',
        'src/api/request.ts',
        'src/constants/{importFieldKeys,importStatusMapping}.ts',
        'src/views/ai-knowledge/composables/*.ts',
        'src/views/ecommerce/composables/*.ts',
        'src/views/ecommerce/{expressCalc,expressPanelView,expressPriceView,monthlySettlementView,listingLinkCardView,listingLinkDate,listingLinkSkuView,salesOrderPanelView}.ts',
        'src/views/notebook/{noteDisplay,noteTreeUtils,exportFolderPdf,importRagNote}.ts',
        'src/views/notebook/composables/*.ts',
      ],
      exclude: [
        'src/main_*.ts',
        'src/bootstrap.ts',
        'src/router/**',
        'src/i18n/**',
        'src/vite-env.d.ts',
        '**/__tests__/**',
      ],
      // 阶段 7：收敛集（阶段 1/5/6 拆分并已测的纯函数/composable/store）整体四维已达 80%+，
      // 门禁直接抬到 80% 防回归；后续新文件随补测达标再逐个加入 include。
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 80,
        statements: 80,
      },
    },
  },
})
