package com.quasar.art.service.impl;

import com.quasar.art.entity.Paper.Paper;
import com.quasar.art.entity.Paper.PaperAiAnalysis;
import com.quasar.art.entity.Paper.PaperEmbedding;
import com.quasar.art.repository.Paper.PaperAiAnalysisRepository;
import com.quasar.art.repository.Paper.PaperEmbeddingRepository;
import com.quasar.art.repository.Paper.PaperRepository;
import com.quasar.art.service.PaperEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaperEmbeddingServiceImpl implements PaperEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(PaperEmbeddingServiceImpl.class);

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private PaperAiAnalysisRepository paperAiAnalysisRepository;

    @Autowired
    private PaperEmbeddingRepository paperEmbeddingRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ai.python.api.url}")
    private String pythonApiUrl;

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    @Override
    public void generateAndSaveEmbedding(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在: " + paperId));

        if (paper.getParseStatus() != 2) {
            throw new RuntimeException("论文尚未解析完成");
        }

        PaperAiAnalysis analysis = paperAiAnalysisRepository.findByPaperId(paperId);
        if (analysis == null) {
            throw new RuntimeException("未找到论文解析数据");
        }

        String coreText = buildCoreText(analysis);
        double[] embedding = callEmbeddingApi(coreText);

        saveEmbedding(paperId, embedding, EMBEDDING_MODEL, embedding.length);
    }

    @Override
    public int batchGenerateEmbedding(List<Long> paperIds) {
        int successCount = 0;
        for (Long paperId : paperIds) {
            try {
                if (!paperEmbeddingRepository.existsByPaperId(paperId)) {
                    generateAndSaveEmbedding(paperId);
                    successCount++;
                }
            } catch (Exception e) {
                log.error("生成论文 {} 的向量失败: {}", paperId, e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public boolean hasEmbedding(Long paperId) {
        return paperEmbeddingRepository.existsByPaperId(paperId);
    }

    private String buildCoreText(PaperAiAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        if (analysis.getResearchQuestion() != null && !analysis.getResearchQuestion().isEmpty()) {
            sb.append(analysis.getResearchQuestion());
        }
        if (analysis.getConclusion() != null && !analysis.getConclusion().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(analysis.getConclusion());
        }
        return sb.toString();
    }

    private double[] callEmbeddingApi(String text) {
        String url = pythonApiUrl.replace("/parse", "/embedding");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);
        requestBody.put("model", EMBEDDING_MODEL);

        log.info("调用 Python Embedding API，文本长度: {}", text.length());

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        if (response == null || (Integer) response.get("code") != 200) {
            throw new RuntimeException("Embedding API 调用失败: " + response);
        }

        List<Double> embeddingList = (List<Double>) response.get("data");
        double[] embedding = new double[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i);
        }

        log.info("Embedding API 调用成功，向量维度: {}", embedding.length);
        return embedding;
    }

    private void saveEmbedding(Long paperId, double[] embedding, String modelName, int dimension) {
        try {
            String embeddingJson = objectMapper.writeValueAsString(embedding);

            PaperEmbedding paperEmbedding = paperEmbeddingRepository.findByPaperId(paperId).orElse(new PaperEmbedding());
            paperEmbedding.setPaperId(paperId);
            paperEmbedding.setEmbeddingVector(embeddingJson);
            paperEmbedding.setModelName(modelName);
            paperEmbedding.setVectorDimension(dimension);

            paperEmbeddingRepository.save(paperEmbedding);
            log.info("论文 {} 的向量已保存", paperId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("向量序列化失败", e);
        }
    }
}