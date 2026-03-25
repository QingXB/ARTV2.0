package com.quasar.art.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.quasar.art.entity.Paper.Paper;
import com.quasar.art.entity.Paper.ReviewTask;

public interface PaperService {
    // 处理文件上传并保存到数据库
    Paper uploadPaper(MultipartFile file, Long userId);
    // 获取用户的文献列表
    List<Paper> getUserPapers(Long userId);
    // 新增：根据文献 ID 重新触发 AI 解析
    void triggerAiAnalysis(Long paperId);
    // 新增：删除文献
    void deletePaper(Long paperId);
    // 新增：获取文献的 AI 解析结果
    com.quasar.art.entity.Paper.PaperAiAnalysis getPaperAnalysis(Long paperId);
    String generateOutline(List<Long> paperIds);
    // 1. 创建异步任务
    ReviewTask createReviewTask(List<Long> paperIds, Long userId);

// 2. 异步执行大模型呼叫
    void startAsyncGenerate(Long taskId, List<Long> paperIds);
    


}