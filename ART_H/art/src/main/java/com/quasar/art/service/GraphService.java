package com.quasar.art.service;

import com.quasar.art.dto.GraphDTO;
import java.util.List;

public interface GraphService {

    GraphDTO calculateSimilarity(Long userId, double threshold);

    GraphDTO calculateSimilarityByPaperIds(List<Long> paperIds, double threshold);

    // 向量接口保留，但内部不依赖AI
    void generateAndSaveEmbedding(Long paperId);
    int batchGenerateEmbedding(List<Long> paperIds);
    boolean hasEmbedding(Long paperId);
}