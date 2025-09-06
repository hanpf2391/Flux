import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // Proxy API requests to the backend server
      '/api': {
        target: 'http://localhost:8080', // Your Spring Boot backend address
        changeOrigin: true, // Needed for virtual hosted sites
      },
      // Proxy WebSocket connections
      '/ws': {
        target: 'ws://localhost:8080', // Your Spring Boot WebSocket address
        ws: true, // IMPORTANT: enable WebSocket proxy
      },
    },
  },
})