package com.quasar.art.controller;

import com.quasar.art.dto.GraphDTO;
import com.quasar.art.dto.OutlineRequestDTO;
import com.quasar.art.service.impl.GraphServiceImpl;
import com.quasar.art.util.JwtUtil;
import com.quasar.art.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin
public class GraphController {

    private static final Logger log = LoggerFactory.getLogger(GraphController.class);

    @Autowired
    private GraphServiceImpl graphService;  // 这里直接用实现类

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @GetMapping("/similarity")
    public Result<GraphDTO> getSimilarityGraph(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0.5") double threshold) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            GraphDTO graph = graphService.calculateSimilarity(userId, threshold);
            log.info("用户 {} 获取相似度图谱：节点{}，边{}",
                    userId, graph.getNodes().size(), graph.getEdges().size());
            return Result.success(graph);
        } catch (Exception e) {
            log.error("图谱生成失败", e);
            return Result.error("图谱生成失败：" + e.getMessage());
        }
    }

    @PostMapping("/similarity/batch")
    public Result<GraphDTO> getSimilarityGraphByPaperIds(
            @RequestBody OutlineRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0.5") double threshold) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        if (request.getPaperIds() == null || request.getPaperIds().isEmpty()) {
            return Result.error("请提供论文ID");
        }
        try {
            GraphDTO graph = graphService.calculateSimilarityByPaperIds(request.getPaperIds(), threshold);
            return Result.success(graph);
        } catch (Exception e) {
            log.error("图谱生成失败", e);
            return Result.error("生成失败：" + e.getMessage());
        }
    }

    // ====================== 向量生成接口（已对齐）======================
    @PostMapping("/embedding/generate/{paperId}")
    public Result<String> generateEmbedding(
            @PathVariable Long paperId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录");
        }
        try {
            graphService.generateAndSaveEmbedding(paperId);
            return Result.success("向量生成并保存成功");
        } catch (Exception e) {
            log.error("向量生成失败", e);
            return Result.error("向量生成失败：" + e.getMessage());
        }
    }

    @PostMapping("/embedding/batch")
    public Result<String> batchGenerateEmbedding(
            @RequestBody OutlineRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录");
        }
        if (request.getPaperIds() == null || request.getPaperIds().isEmpty()) {
            return Result.error("请提供论文ID列表");
        }
        try {
            int count = graphService.batchGenerateEmbedding(request.getPaperIds());
            return Result.success("成功生成 " + count + " 个向量");
        } catch (Exception e) {
            return Result.error("批量生成失败：" + e.getMessage());
        }
    }

    @GetMapping("/embedding/{paperId}")
    public Result<Boolean> checkEmbeddingExists(
            @PathVariable Long paperId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录");
        }
        return Result.success(graphService.hasEmbedding(paperId));
    }
}