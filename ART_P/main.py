from fastapi import FastAPI
import uvicorn
from api import router  # 引入路由模块

app = FastAPI()

# 挂载路由，统一加上 /api/ai 前缀
app.include_router(router, prefix="/api/ai")

if __name__ == "__main__":
    print("🚀 智研 Python AI 微服务启动成功！监听端口: 8000")
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)