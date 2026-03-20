package com.quasar.art.service;

import org.springframework.web.multipart.MultipartFile;
import com.quasar.art.entity.Paper.Paper;

public interface PaperService {
    // 处理文件上传并保存到数据库
    Paper uploadPaper(MultipartFile file, Long userId);
}