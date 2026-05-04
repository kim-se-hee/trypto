import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

const apiTarget = process.env.VITE_API_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': apiTarget,
      '/ws': {
        target: apiTarget,
        ws: true,
      },
    },
  },
  build: {
    outDir: '../resources/static',
    emptyOutDir: true,
  },
})
