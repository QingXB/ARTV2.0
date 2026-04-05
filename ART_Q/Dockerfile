# ART_Q - Vue 前端（开发模式）
FROM node:20-alpine

WORKDIR /app

# 复制 package.json
COPY ART_Q/package*.json ./

# 安装依赖
RUN npm install

# 复制源代码
COPY ART_Q/ .

# 暴露端口
EXPOSE 5173

# 环境变量
ENV VITE_APP_PDF_URL=http://localhost:8080/pdf/

# 启动命令（开发模式）
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
