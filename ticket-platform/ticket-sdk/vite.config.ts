import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'TicketSDK',
      formats: ['umd', 'es'],
      fileName: (format) => (format === 'es' ? 'ticket-sdk.es.js' : 'ticket-sdk.umd.js'),
    },
    rollupOptions: {
      output: {
        exports: 'named',
      },
    },
    // 保留 SDK 公共产物中的函数名。该脚本会被多种宿主和 CDN 再处理，二次压缩曾导致
    // 工单详情的局部短变量遮蔽格式化函数，最终只暴露 `d is not defined/function`。
    minify: false,
    sourcemap: true,
  },
})
