package com.quasar.art.service.impl;

import com.quasar.art.entity.Paper.Paper;
import com.quasar.art.repository.Paper.PaperRepository;
import com.quasar.art.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperRepository paperRepository;

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
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            
            // 为了防止重名，使用 UUID 生成一个新的物理文件名
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // 3. 把文件真正存入硬盘
            Path targetLocation = uploadPath.resolve(newFileName);
            file.transferTo(targetLocation.toFile());

            // 4. 将记录存入 PostgreSQL 数据库
            Paper paper = new Paper();
            paper.setUserId(userId);
            paper.setTitle(originalFilename); // 暂用原文件名当标题，后续可以被 AI 修正
            paper.setFilePath(targetLocation.toString()); // 存下这个文件在硬盘上的绝对位置
            paper.setParseStatus(0); // 0代表未解析，等着 Python 来接手

            // 保存到数据库并返回
            return paperRepository.save(paper);

        } catch (IOException ex) {
            throw new RuntimeException("无法保存文件，请检查目录权限!", ex);
        }
    }
}