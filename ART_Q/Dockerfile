# ART_Q - Vue 前端（生产模式）
# 构建阶段：用 Node 编译 Vue 项目
FROM node:20-alpine AS builder

WORKDIR /app

# 复制 package.json 先安装依赖（利用 Docker 缓存层）
COPY ART_Q/package*.json ./
RUN npm config set registry https://registry.npmmirror.com && npm install

# 复制源代码
COPY ART_Q/ .

# 通过 nginx 代理 API 请求，前端不需要写死服务器地址
ENV VITE_APP_API_URL=
ENV VITE_APP_PDF_URL=/pdf/

# 构建生产版本（输出到 dist/）
RUN npm run build

# ============================================================
# 运行阶段：用 Nginx 提供静态文件
# ============================================================
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY ART_Q/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
