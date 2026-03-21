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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private RestTemplate restTemplate;

    // 🌟 把你的 AI 解析 Repository 注入进来
    @Autowired
    private com.quasar.art.repository.Paper.PaperAiAnalysisRepository aiAnalysisRepository;
    
    @Value("${ai.python.api.url}")
    private String pythonApiUrlConfig;

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
    Map response = restTemplate.postForObject(pythonApiUrlConfig, requestBody, Map.class);
    
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

    @Override
    public void triggerAiAnalysis(Long paperId) {
        // 1. 去数据库把这篇论文找出来
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("找不到对应的文献数据！"));

        // 2. 开启异步线程去呼叫 Python
        new Thread(() -> {
            try {
                // 拼接出文件的绝对物理路径
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Path targetLocation = uploadPath.resolve(paper.getFilePath());
                String absolutePath = targetLocation.toAbsolutePath().toString();

                System.out.println("🚀 [手动触发解析] 准备发送给 Python 的文件路径: " + absolutePath);

                // 组装数据并发送给 Python
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("file_path", absolutePath);


// ... 前面的发送请求代码 ...
Map response = restTemplate.postForObject(pythonApiUrlConfig, requestBody, Map.class);
System.out.println("🎉 [手动触发解析] 收到 Python 的回信: " + response);

// ====== 🌟 核心绝杀：解析回信并精准落库（适配你的 Repository） ======
if (response != null && (Integer) response.get("code") == 200) {
    // 1. 把 data 那一坨 Map 挖出来
    Map<String, String> data = (Map<String, String>) response.get("data");

    // 2. 查出旧数据，如果没有就 new 一个新的
    com.quasar.art.entity.Paper.PaperAiAnalysis aiData = aiAnalysisRepository.findByPaperId(paperId);
    if (aiData == null) {
        aiData = new com.quasar.art.entity.Paper.PaperAiAnalysis();
        aiData.setPaperId(paperId); // 只有新纪录才需要绑定 paperId
    }
    
    // 塞入 AI 提取的三个核心字段
    aiData.setResearchQuestion(data.get("research_question"));
    aiData.setMethodology(data.get("methodology"));
    aiData.setConclusion(data.get("conclusion"));
    
    // 🌟 核心：用 Jackson 把原始 data 转成 JSON 字符串，存入你的 jsonb 字段
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    aiData.setRawAiResponse(mapper.writeValueAsString(data));

    // 保存到数据库
    aiAnalysisRepository.save(aiData);

    // 3. 把主表 (Paper) 的状态改为 2 (已解析)
    paper.setParseStatus(2);
    paperRepository.save(paper);

    System.out.println("✅ [Java 线程] 完美入库！数据已成功落入 PostgreSQL，主表状态已更新为已解析！");
} else {
    System.err.println("⚠️ [Java 线程] Python 返回的状态码不是 200，解析可能异常！");
}
// ========================================

} catch (Exception e) {
System.err.println("❌ [手动触发解析] 呼叫 Python 失败或落库异常: " + e.getMessage());
e.printStackTrace(); 
}
}).start();
    }
    @Override
    public void deletePaper(Long paperId) {
        // 1. 先查出这篇文献，我们需要拿到它的 filePath 才知道去硬盘哪里删
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("找不到对应的文献数据！"));

        // 2. 物理毁灭：去硬盘的 paper 文件夹里把它删掉
        try {
            Path filePath = Paths.get(uploadDir).resolve(paper.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
            System.out.println("🗑️ 成功删除本地物理文件: " + filePath.toString());
        } catch (IOException e) {
            // 就算硬盘上没找到文件，也不要抛异常卡死，继续删数据库就行
            System.err.println("⚠️ 物理文件删除失败或文件不存在: " + e.getMessage());
        }

        // 3. 抹除记录：从 PostgreSQL 数据库里删掉这行数据
        paperRepository.deleteById(paperId);
    }
    @Override
    public com.quasar.art.entity.Paper.PaperAiAnalysis getPaperAnalysis(Long paperId) {
        // 直接调用你的 Repository 查出数据
        return aiAnalysisRepository.findByPaperId(paperId);
    }
}