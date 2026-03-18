import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url' // 🌟 1. 引入 Node.js 处理路径的内置模块

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 🌟 2. 核心魔法：告诉 Vite，以后看到 '@'，就直接翻译成绝对路径的 'src' 目录
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})