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
    minify: 'esbuild',
    sourcemap: true,
  },
})
