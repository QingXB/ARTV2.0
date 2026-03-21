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
}