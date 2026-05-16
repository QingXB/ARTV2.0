package com.quasar.art.service;

import java.util.List;

public interface PaperEmbeddingService {
    
    void generateAndSaveEmbedding(Long paperId);
    
    int batchGenerateEmbedding(List<Long> paperIds);
    
    boolean hasEmbedding(Long paperId);
}