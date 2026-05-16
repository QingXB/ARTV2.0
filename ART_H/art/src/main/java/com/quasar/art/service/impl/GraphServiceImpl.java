package com.quasar.art.service.impl;

import com.quasar.art.dto.GraphDTO;
import com.quasar.art.entity.Paper.Paper;
import com.quasar.art.entity.Paper.PaperAiAnalysis;
import com.quasar.art.entity.Paper.PaperEmbedding;
import com.quasar.art.repository.Paper.PaperAiAnalysisRepository;
import com.quasar.art.repository.Paper.PaperEmbeddingRepository;
import com.quasar.art.repository.Paper.PaperRepository;
import com.quasar.art.service.GraphService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphServiceImpl implements GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphServiceImpl.class);

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private PaperEmbeddingRepository paperEmbeddingRepository;

    @Autowired
    private PaperAiAnalysisRepository paperAiAnalysisRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // ====================== 图谱计算（原有，正常运行）======================
    @Override
    public GraphDTO calculateSimilarity(Long userId, double threshold) {
        List<Paper> papers = paperRepository.findByUserId(userId);
        List<Long> paperIds = papers.stream()
                .filter(p -> p.getParseStatus() == 2)
                .map(Paper::getId)
                .collect(Collectors.toList());
        return calculateSimilarityByPaperIds(paperIds, threshold);
    }

    @Override
    public GraphDTO calculateSimilarityByPaperIds(List<Long> paperIds, double threshold) {
        log.info("开始计算 {} 篇文献的语义相似度，阈值: {}", paperIds.size(), threshold);

        List<Paper> papers = paperRepository.findAllById(paperIds);
        Map<Long, Paper> paperMap = papers.stream()
                .collect(Collectors.toMap(Paper::getId, p -> p));

        List<PaperEmbedding> embeddings = paperEmbeddingRepository.findByPaperIdIn(paperIds);
        Map<Long, double[]> embeddingMap = new HashMap<>();

        for (PaperEmbedding embedding : embeddings) {
            try {
                double[] vector = parseEmbeddingVector(embedding.getEmbeddingVector());
                if (vector != null) {
                    embeddingMap.put(embedding.getPaperId(), vector);
                }
            } catch (Exception e) {
                log.error("论文 {} 向量解析失败", embedding.getPaperId());
            }
        }

        Map<Long, String> summaryMap = new HashMap<>();
        List<PaperAiAnalysis> analyses = paperAiAnalysisRepository.findByPaperIdIn(paperIds);
        for (PaperAiAnalysis analysis : analyses) {
            summaryMap.put(analysis.getPaperId(), buildSummary(analysis));
        }

        List<GraphDTO.Node> nodes = new ArrayList<>();
        List<GraphDTO.Edge> edges = new ArrayList<>();
        List<Long> validIds = new ArrayList<>(embeddingMap.keySet());

        for (Long id : validIds) {
            Paper p = paperMap.get(id);
            if (p != null) {
                nodes.add(GraphDTO.Node.builder()
                        .id(id)
                        .name("Paper_" + id)
                        .title(p.getTitle())
                        .summary(summaryMap.getOrDefault(id, ""))
                        .author(p.getAuthor())
                        .publishYear(p.getPublishYear())
                        .parseStatus(p.getParseStatus())
                        .category(0)
                        .build());
            }
        }

        for (int i = 0; i < validIds.size(); i++) {
            for (int j = i + 1; j < validIds.size(); j++) {
                Long a = validIds.get(i);
                Long b = validIds.get(j);
                double[] v1 = embeddingMap.get(a);
                double[] v2 = embeddingMap.get(b);

                if (v1 == null || v2 == null) continue;

                double sim = cosineSimilarity(v1, v2);
                if (sim >= threshold) {
                    edges.add(GraphDTO.Edge.builder()
                            .source(String.valueOf(a))
                            .target(String.valueOf(b))
                            .weight(sim)
                            .relationType("SIMILAR")
                            .build());
                }
            }
        }

        log.info("图谱构建完成：{} 节点，{} 边", nodes.size(), edges.size());
        return GraphDTO.builder().nodes(nodes).edges(edges).build();
    }

    // ====================== 模拟向量（不依赖AI，直接生成测试向量）======================
    @Transactional
    @Override
    public void generateAndSaveEmbedding(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        if (paperEmbeddingRepository.existsByPaperId(paperId)) {
            log.info("论文{}已存在向量", paperId);
            return;
        }

        // 生成测试向量（本地生成，不调用AI）
        List<Double> testVector = generateTestVector(384);

        try {
            String json = objectMapper.writeValueAsString(testVector);

            // 不用 Lombok Builder，手动 new 绝对不报错
            PaperEmbedding em = new PaperEmbedding();
            em.setPaperId(paperId);
            em.setEmbeddingVector(json);
            em.setVectorDimension(384);
            em.setModelName("test-vector");

            paperEmbeddingRepository.save(em);
            log.info("论文{} 测试向量已保存到数据库", paperId);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("向量保存失败");
        }
    }

    // 本地生成测试向量，完全不依赖外部服务
    private List<Double> generateTestVector(int dim) {
        List<Double> vec = new ArrayList<>();
        for (int i = 0; i < dim; i++) {
            vec.add(Math.random() * 2 - 1);
        }
        return vec;
    }

    @Transactional
    @Override
    public int batchGenerateEmbedding(List<Long> paperIds) {
        int count = 0;
        for (Long id : paperIds) {
            try {
                generateAndSaveEmbedding(id);
                count++;
            } catch (Exception e) {
                log.error("论文{} 向量生成失败", id);
            }
        }
        return count;
    }

    @Override
    public boolean hasEmbedding(Long paperId) {
        return paperEmbeddingRepository.existsByPaperId(paperId);
    }

    // ====================== 工具方法 ======================
    private double cosineSimilarity(double[] v1, double[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0;
        double dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }
        if (n1 == 0 || n2 == 0) return 0;
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    private double[] parseEmbeddingVector(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Double[] arr = objectMapper.readValue(json, Double[].class);
            double[] res = new double[arr.length];
            for (int i = 0; i < arr.length; i++) res[i] = arr[i];
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildSummary(PaperAiAnalysis analysis) {
        if (analysis == null) return "";
        StringBuilder sb = new StringBuilder();
        if (analysis.getResearchQuestion() != null) sb.append("【问题】").append(analysis.getResearchQuestion());
        if (analysis.getConclusion() != null) sb.append("【结论】").append(analysis.getConclusion());
        String s = sb.toString();
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}