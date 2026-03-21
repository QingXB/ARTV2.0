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

@RestController
@RequestMapping("/api/papers")
@CrossOrigin 
public class PaperController {

    @Autowired
    private PaperService paperService;

    // 🌟 注入 UserRepository，用来查字典
    @Autowired
    private UserRepository userRepository;

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

            // 2. 提取出用户名 (假设你的 token 是 "quasar-auth-token-Quasar" 或者直接就是 "Quasar")
            String username = token.replace("quasar-auth-token-", "");

            // 3. 🌟 核心转变：去数据库查这个用户名到底是谁！
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return Result.error("查无此人：非法的用户名！");
            }

            // 4. 拿到真实的数字 ID
            Long currentUserId = user.getId();

            // 5. 存入数据库
            Paper savedPaper = paperService.uploadPaper(file, currentUserId);
            return Result.success(savedPaper);

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
            // 2. 提取用户名
            String username = token.replace("quasar-auth-token-", "");

            // 3. 去数据库查这个用户名对应的真实 User
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return Result.error("查无此人：非法的用户名！");
            }

            // 4. 去文献表里查该用户上传的所有文献
            List<Paper> papers = paperService.getUserPapers(user.getId());
            
            return Result.success(papers);

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
}