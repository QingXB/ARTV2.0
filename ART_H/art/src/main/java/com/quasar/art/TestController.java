package com.quasar.art;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping(value = "/test", produces = MediaType.TEXT_HTML_VALUE)
    public String testPage() {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <title>Spring Boot 测试页</title>
                    <style>
                        body { font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background:#0f172a; color:#e5e7eb; display:flex; align-items:center; justify-content:center; height:100vh; margin:0; }
                        .card { background:rgba(15,23,42,0.9); padding:32px 40px; border-radius:18px; box-shadow:0 24px 60px rgba(15,23,42,0.8); max-width:440px; text-align:center; border:1px solid rgba(148,163,184,0.35); }
                        h1 { font-size:24px; margin:0 0 12px; letter-spacing:0.06em; text-transform:uppercase; color:#a5b4fc; }
                        p { margin:4px 0; font-size:14px; color:#cbd5f5; }
                        .status { margin-top:16px; display:inline-flex; align-items:center; gap:8px; padding:6px 14px; border-radius:999px; background:rgba(34,197,94,0.12); border:1px solid rgba(34,197,94,0.4); color:#bbf7d0; font-size:12px; letter-spacing:0.08em; text-transform:uppercase; }
                        .dot { width:8px; height:8px; border-radius:999px; background:#22c55e; box-shadow:0 0 12px rgba(34,197,94,0.9); }
                        .hint { margin-top:20px; font-size:12px; color:#9ca3af; }
                        code { background:rgba(15,23,42,0.9); padding:2px 6px; border-radius:4px; border:1px solid rgba(148,163,184,0.5); font-size:12px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Spring Boot 正常运行</h1>
                        <p>当前应用已经成功启动，并且可以返回网页响应。</p>
                        <p>如果你能看到这个页面，说明后端服务是 <strong>可用</strong> 的。</p>
                        <div class="status">
                            <div class="dot"></div>
                            <span>Service Status · OK</span>
                        </div>
                        <div class="hint">
                            <p>你正在访问接口：<code>GET /test</code></p>
                        </div>
                    </div>
                </body>
                </html>
                """;
    }
}

