package com.quasar.art.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.quasar.art.dto.PageDTO;
import com.quasar.art.entity.Paper.Paper;
import com.quasar.art.entity.Paper.ReviewTask;

public interface PaperService {
    Paper uploadPaper(MultipartFile file, Long userId);
    PageDTO<Paper> getUserPapers(Long userId, int page, int size, String keyword, Integer status);
    List<Paper> getUserPapers(Long userId);
    void triggerAiAnalysis(Long paperId);
    int triggerBatchAnalysis(Long userId);
    void deletePaper(Long paperId);
    com.quasar.art.entity.Paper.PaperAiAnalysis getPaperAnalysis(Long paperId);
    String generateOutline(List<Long> paperIds);
    ReviewTask createReviewTask(List<Long> paperIds, Long userId);
    void startAsyncGenerate(Long taskId, List<Long> paperIds);
    java.util.List<com.quasar.art.entity.Paper.PaperRelationship> analyzePaperRelations(java.util.List<Long> paperIds);
    void batchDeletePapers(java.util.List<Long> paperIds);
}