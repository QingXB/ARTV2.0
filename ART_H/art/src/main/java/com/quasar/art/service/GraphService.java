package com.quasar.art.service;

import com.quasar.art.dto.GraphDTO;
import java.util.List;

public interface GraphService {

    GraphDTO calculateSimilarity(Long userId, double threshold);

    GraphDTO calculateSimilarityByPaperIds(List<Long> paperIds, double threshold);
}