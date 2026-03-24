package com.quasar.art.controller;

import com.quasar.art.entity.Paper.*;
import com.quasar.art.entity.User;
import com.quasar.art.repository.UserRepository;
import com.quasar.art.service.PaperService;
import com.quasar.art.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; // 记得在文件顶部导包
import com.quasar.art.dto.OutlineRequestDTO; // 导入刚才建的 DTO

@RestController
@RequestMapping("/api/papers")
@CrossOrigin 
public class PaperController {

    @Autowired
    private PaperService paperService;


    @PostMapping("/upload")
    public Result<Paper> uploadPaper(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("非法请求：未登录或 Token 丢失！");
            }

            // 1. 拿到纯净的 token 字符串
            String token = authHeader.substring(7); 

            // 2. 🌟 提取出真实的数字 ID 字符串 (例如剥离后剩下 "1")
            String userIdStr = token.replace("quasar-auth-token-", "");

            // 3. 🌟 将字符串转成纯数字 Long 类型
            Long currentUserId = Long.parseLong(userIdStr);

            // 4. 🚀 直接拿着 ID 去上传存库！跳过 User 表查询，性能拉满！
            Paper savedPaper = paperService.uploadPaper(file, currentUserId);
            return Result.success(savedPaper);

        } catch (NumberFormatException e) {
            // 防呆校验：如果前端乱传 Token，抠出来的不是纯数字，直接拦截
            return Result.error("身份验证失败：非法的 Token 标识！");
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
    @GetMapping("/list")
    public Result<List<Paper>> getPaperList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("非法请求：未登录或 Token 丢失！");
            }

            // 1. 拿纯净 Token
            String token = authHeader.substring(7); 
            
            // 2. 🌟 提取真正的 ID (剥去前缀)
            String userIdStr = token.replace("quasar-auth-token-", "");

            // 3. 将提取出来的字符串转换为 Long 类型
            Long userId = Long.parseLong(userIdStr);

            // 4. 🌟 直接拿 ID 去查用户的文献！(甚至都不需要去查 User 表验证了，少查一次数据库，性能翻倍！)
            List<Paper> papers = paperService.getUserPapers(userId);
            
            return Result.success(papers);

        } catch (NumberFormatException e) {
            // 如果提取出来的不是数字，说明 Token 被乱改了
            return Result.error("Token格式异常：非法的身份标识！");
        } catch (Exception e) {
            return Result.error("获取文献列表失败：" + e.getMessage());
        }
    }
    // 🌟 新增：手动触发解析的接口
    @PostMapping("/{id}/parse")
    public Result<String> parsePaper(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 这里你可以复用之前写过的 token 鉴权逻辑，确保是登录用户在操作
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("非法请求：未登录！");
            }

            // 直接呼叫 Service 层的触发方法
            paperService.triggerAiAnalysis(id);
            
            // 因为解析是后台异步进行的，所以我们直接告诉前端“任务已提交”
            return Result.success("解析任务已提交至后台，请稍候...");
        } catch (Exception e) {
            return Result.error("触发解析失败：" + e.getMessage());
        }
    }
    // 🌟 新增：删除文献接口
    @DeleteMapping("/{id}")
    public Result<String> deletePaper(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("非法请求：未登录！");
            }
            // 调用 Service 层去删文件和数据
            paperService.deletePaper(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }
    // 🌟 新增：获取单篇文献的 AI 解析详情
    @GetMapping("/{id}/analysis")
    public Result<com.quasar.art.entity.Paper.PaperAiAnalysis> getPaperAnalysis(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("非法请求：未登录！");
            }
            
            com.quasar.art.entity.Paper.PaperAiAnalysis analysis = paperService.getPaperAnalysis(id);
            
            // 如果没查到，说明还没解析，返回 null 让前端显示"暂无数据"
            return Result.success(analysis); 
        } catch (Exception e) {
            return Result.error("获取解析详情失败：" + e.getMessage());
        }
    }
    @PostMapping("/generate-outline")
    public Result<String> generateOutline(@RequestBody OutlineRequestDTO request) {
        if (request.getPaperIds() == null || request.getPaperIds().size() < 2) {
            return Result.error("至少需要 2 篇文献才能生成综述对比！");
        }
        String outlineMarkdown = paperService.generateOutline(request.getPaperIds());
        return Result.success(outlineMarkdown);
    }
    // 1. 提交任务：前端点按钮后调这个
@PostMapping("/generate-async")
public Result<Long> submitTask(@RequestBody OutlineRequestDTO request) {
    // 创建一条状态为 0 的任务记录并存入数据库
    ReviewTask task = paperService.createReviewTask(request.getPaperIds());
    // 🌟 异步执行真正的 AI 生成逻辑（不等待，直接往下走）
    paperService.startAsyncGenerate(task.getId(), request.getPaperIds());
    // 返回任务 ID 给前端
    return Result.success(task.getId());
}



}   
