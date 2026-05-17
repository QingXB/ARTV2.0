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
import org.springframework.transaction.annotation.Transactional;
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

    // 尝试多种通用的embedding模型名称，大多数中转商都支持
    private static final String EMBEDDING_MODEL = "text-embedding-ada-002";

    @Override
    public void generateAndSaveEmbedding(Long paperId) {
        // 节省API：先检查是否已存在向量
        if (paperEmbeddingRepository.existsByPaperId(paperId)) {
            log.info("论文 {} 已存在向量，跳过API调用", paperId);
            return;
        }

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

    @Override
    @Transactional
    public int regenerateEmbeddings(List<Long> paperIds) {
        log.info("开始重新生成 {} 篇文献的向量", paperIds.size());
        
        // 先删除已有的向量
        paperEmbeddingRepository.deleteByPaperIdIn(paperIds);
        log.info("已删除 {} 篇文献的旧向量", paperIds.size());
        
        // 然后重新生成
        int count = 0;
        for (Long paperId : paperIds) {
            try {
                generateAndSaveEmbeddingWithoutCheck(paperId);
                count++;
            } catch (Exception e) {
                log.error("重新生成论文 {} 的向量失败: {}", paperId, e.getMessage());
            }
        }
        log.info("成功重新生成 {} 个向量", count);
        return count;
    }

    // 不检查已存在，直接生成（用于重新生成）
    private void generateAndSaveEmbeddingWithoutCheck(Long paperId) {
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

    private String buildCoreText(PaperAiAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        if (analysis.getResearchQuestion() != null && !analysis.getResearchQuestion().isEmpty()) {
            sb.append(analysis.getResearchQuestion());
        }
        if (analysis.getMethodology() != null && !analysis.getMethodology().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(analysis.getMethodology());
        }
        if (analysis.getConclusion() != null && !analysis.getConclusion().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(analysis.getConclusion());
        }
        String result = sb.toString();
        log.info("生成向量用的核心文本: {}", result);
        return result;
    }

    private double[] callEmbeddingApi(String text) {
        String url = pythonApiUrl.replace("/parse", "/embedding");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);
        requestBody.put("model", EMBEDDING_MODEL);

        log.info("调用 Python Embedding API，URL: {}, 文本长度: {}", url, text.length());

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        log.info("Python API 返回原始响应: {}", response);

        if (response == null) {
            throw new RuntimeException("Embedding API 返回空响应");
        }

        Integer code = (Integer) response.get("code");
        if (code == null || code != 200) {
            throw new RuntimeException("Embedding API 调用失败，code: " + code + ", response: " + response);
        }

        List<Double> embeddingList = (List<Double>) response.get("data");
        if (embeddingList == null || embeddingList.isEmpty()) {
            throw new RuntimeException("Embedding API 返回的向量数据为空");
        }

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