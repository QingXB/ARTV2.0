package com.quasar.art.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import com.quasar.art.entity.Paper.Paper;

public interface PaperService {
    // 处理文件上传并保存到数据库
    Paper uploadPaper(MultipartFile file, Long userId);
    // 获取用户的文献列表
    List<Paper> getUserPapers(Long userId);
}