package com.quasar.art.service.impl;

import com.quasar.art.entity.Paper.Paper;
import com.quasar.art.repository.Paper.PaperRepository;
import com.quasar.art.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private RestTemplate restTemplate;

    // 从 application.properties 里读取配置的相对路径 ("./paper")
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public Paper uploadPaper(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传的 PDF 文件不能为空");
        }

        try {
            // 1. 确保目录存在（如果没有 paper 文件夹，系统会自动建一个）
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. 获取原始文件名并提取后缀 (例如 "attention.pdf" -> ".pdf")
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 为了防止重名，使用 UUID 生成一个新的物理文件名
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // 3. 把文件真正存入硬盘
            Path targetLocation = uploadPath.resolve(newFileName);
            file.transferTo(targetLocation.toFile());

            // 4. 将记录存入 PostgreSQL 数据库
            Paper paper = new Paper();
            paper.setUserId(userId);
            paper.setTitle(originalFilename); // 暂用原文件名当标题，后续可以被 AI 修正
            
            // 🌟 修正 2：核心改变！只存 UUID 文件名，绝对不存 C:\ 等绝对路径
            paper.setFilePath(newFileName); 
            
            paper.setParseStatus(0); // 0代表未解析，等着 Python 来接手

// ====== 🌟 世纪握手测试代码 开始 ======
try {
    // 1. 获取刚刚存好的 PDF 的绝对物理路径
    String absolutePath = targetLocation.toAbsolutePath().toString();
    System.out.println("准备发送给 Python 的文件路径: " + absolutePath);

    // 2. 组装给 Python 的 JSON 数据：{"file_path": "C:\...\xxx.pdf"}
    java.util.Map<String, String> requestBody = new java.util.HashMap<>();
    requestBody.put("file_path", absolutePath);

    // 3. 呼叫 Python 服务
    String pythonApiUrl = "http://127.0.0.1:8000/api/ai/parse";
    java.util.Map response = restTemplate.postForObject(pythonApiUrl, requestBody, java.util.Map.class);
    
    // 4. 打印 Python 的回信
    System.out.println("🎉 收到 Python 的回信: " + response);
    
} catch (Exception e) {
    System.err.println("❌ 呼叫 Python 失败，看看是不是 Python 没启动？报错: " + e.getMessage());
}
// ====== 🌟 世纪握手测试代码 结束 ======

// 保存到数据库并返回
return paperRepository.save(paper);

        } catch (IOException ex) {
            throw new RuntimeException("无法保存文件，请检查目录权限!", ex);
        }
    }
    

    @Override
    public List<Paper> getUserPapers(Long userId) {
        // 直接调用 Repository 里我们早就写好的方法，底层会自动生成 SQL 去查
        return paperRepository.findByUserId(userId);
    }
    
}