import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  // `npm run build && npm run preview` serves the built app with no HMR client,
  // which is what you want when checking behaviour on a real phone — a dropped
  // websocket on a sleeping handset makes the dev server reload the page.
  preview: {
    port: 4173,
    host: true,
  },
  server: {
    port: 5173,
    // Listen on every interface so a phone on the same wifi can open the app and
    // the mobile layout can be checked on real hardware.
    host: true,
    // Keeps the browser on one origin in development, so no CORS round trips.
    // It also means a phone only needs port 5173 — Vite reaches the backend
    // server-side, so 8080 never has to leave this machine.
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
