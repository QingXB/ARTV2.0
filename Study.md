后端目录详情
com.quasar.art
├── config       // 配置信息 (AI API, Swagger等)
├── controller   // 控制层 (API 接口)
├── dto          // 数据传输对象 (输入输出模型)
├── entity       // 实体类 (数据库表映射)
├── repository   // 数据库操作 (JPA/MyBatis)
├── service      // 业务逻辑 (核心 AI/解析逻辑)
│   └── impl     // 业务实现
└── util         // 通用工具类 (PDF处理、字符串处理)

python 微服务的目录结构
ART_P/                  # Python 微服务根目录
├── venv/               # 📦 虚拟环境 (存放依赖包，不用管它，绝对不要传 Git)
├── .env                # 🔒 环境变量文件 (存放你的 sk- 密钥，已加入 .gitignore)
├── .gitignore          # 🛡️ Git 忽略名单 (里面写了 .env 和 venv/)
├── config.py           # ⚙️ 配置文件 (负责关闭代理干扰、读取密钥、初始化 Client)
├── api.py              # 🧠 核心业务 (负责写具体的 /parse 和 /generate-outline 逻辑)
└── main.py             # 🚀 启动入口 (极其精简，只负责把 api.py 挂载上来并启动服务)