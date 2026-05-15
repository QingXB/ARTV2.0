package com.quasar.art.controller;

import com.quasar.art.dto.PageDTO;
import com.quasar.art.entity.Paper.*;
import com.quasar.art.repository.Paper.ReviewTaskRepository;
import com.quasar.art.service.PaperService;
import com.quasar.art.util.JwtUtil;
import com.quasar.art.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import com.quasar.art.dto.OutlineRequestDTO;

@RestController
@RequestMapping("/api/papers")
@CrossOrigin
public class PaperController {

    private static final Logger log = LoggerFactory.getLogger(PaperController.class);

    @Autowired
    private PaperService paperService;

    @Autowired
    private ReviewTaskRepository reviewTaskRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 通用方法：从请求头获取userId
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping("/upload")
    public Result<Paper> uploadPaper(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            Paper savedPaper = paperService.uploadPaper(file, userId);
            log.info("用户 {} 上传文献: {}", userId, savedPaper.getTitle());
            return Result.success(savedPaper);
        } catch (Exception e) {
            log.error("上传失败: {}", e.getMessage());
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<PageDTO<Paper>> getPaperList(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            PageDTO<Paper> result = paperService.getUserPapers(userId, page, size, keyword, status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文献列表失败: {}", e.getMessage());
            return Result.error("获取文献列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/parse")
    public Result<String> parsePaper(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            paperService.triggerAiAnalysis(id);
            log.info("用户 {} 触发解析文献: {}", userId, id);
            return Result.success("解析任务已提交至后台，请稍候...");
        } catch (Exception e) {
            log.error("触发解析失败: {}", e.getMessage());
            return Result.error("触发解析失败：" + e.getMessage());
        }
    }

    @PostMapping("/parse-all")
    public Result<String> parseAllPapers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            int count = paperService.triggerBatchAnalysis(userId);
            log.info("用户 {} 批量解析 {} 篇文献", userId, count);
            return Result.success("已提交 " + count + " 篇文献的解析任务");
        } catch (Exception e) {
            log.error("批量解析失败: {}", e.getMessage());
            return Result.error("批量解析失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> deletePaper(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            paperService.deletePaper(id);
            log.info("用户 {} 删除文献: {}", userId, id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除失败: {}", e.getMessage());
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}/analysis")
    public Result<PaperAiAnalysis> getPaperAnalysis(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            PaperAiAnalysis analysis = paperService.getPaperAnalysis(id);
            return Result.success(analysis);
        } catch (Exception e) {
            log.error("获取解析详情失败: {}", e.getMessage());
            return Result.error("获取解析详情失败：" + e.getMessage());
        }
    }

    @PostMapping("/generate-outline")
    public Result<String> generateOutline(@RequestBody OutlineRequestDTO request) {
        if (request.getPaperIds() == null || request.getPaperIds().size() < 2) {
            return Result.error("至少需要 2 篇文献才能生成综述对比！");
        }
        try {
            String outlineMarkdown = paperService.generateOutline(request.getPaperIds());
            return Result.success(outlineMarkdown);
        } catch (Exception e) {
            log.error("生成综述失败: {}", e.getMessage());
            return Result.error("生成综述失败：" + e.getMessage());
        }
    }

    @PostMapping("/generate-async")
    public Result<Long> submitTask(
            @RequestBody OutlineRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        if (request.getPaperIds() == null || request.getPaperIds().size() < 2) {
            return Result.error("至少需要 2 篇文献才能生成综述！");
        }
        try {
            ReviewTask task = paperService.createReviewTask(request.getPaperIds(), userId);
            paperService.startAsyncGenerate(task.getId(), request.getPaperIds());
            log.info("用户 {} 创建综述任务: {}", userId, task.getId());
            return Result.success(task.getId());
        } catch (Exception e) {
            log.error("创建综述任务失败: {}", e.getMessage());
            return Result.error("创建综述任务失败：" + e.getMessage());
        }
    }

    @GetMapping("/task-status/{taskId}")
    public Result<ReviewTask> getTaskStatus(@PathVariable("taskId") Long taskId) {
        try {
            ReviewTask task = reviewTaskRepository.findById(taskId).orElse(null);
            if (task == null) {
                return Result.error("查无此任务");
            }
            return Result.success(task);
        } catch (Exception e) {
            log.error("查询任务状态失败: {}", e.getMessage());
            return Result.error("查询状态发生异常: " + e.getMessage());
        }
    }

    @GetMapping("/review-history")
    public Result<List<ReviewTask>> getReviewHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        try {
            List<ReviewTask> history = reviewTaskRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage());
            return Result.error("获取历史记录失败：" + e.getMessage());
        }
    }

    @PostMapping("/analyze-relations")
    public Result<List<PaperRelationship>> analyzeRelations(
            @RequestBody OutlineRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        if (request.getPaperIds() == null || request.getPaperIds().size() < 2) {
            return Result.error("至少需要 2 篇文献才能分析关系！");
        }
        try {
            List<PaperRelationship> relations = paperService.analyzePaperRelations(request.getPaperIds());
            log.info("用户 {} 分析 {} 篇文献关系", userId, request.getPaperIds().size());
            return Result.success(relations);
        } catch (Exception e) {
            log.error("关系分析失败: {}", e.getMessage());
            return Result.error("关系分析失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public Result<String> batchDeletePapers(
            @RequestBody OutlineRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        if (userId == null) {
            return Result.error("未登录或Token无效");
        }
        if (request.getPaperIds() == null || request.getPaperIds().isEmpty()) {
            return Result.error("请选择要删除的文献！");
        }
        try {
            paperService.batchDeletePapers(request.getPaperIds());
            log.info("用户 {} 批量删除 {} 篇文献", userId, request.getPaperIds().size());
            return Result.success("批量删除成功，共删除 " + request.getPaperIds().size() + " 篇文献");
        } catch (Exception e) {
            log.error("批量删除失败: {}", e.getMessage());
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }
}