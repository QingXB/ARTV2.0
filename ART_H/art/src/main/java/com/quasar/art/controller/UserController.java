package com.quasar.art.controller;

import com.quasar.art.dto.LoginDTO;
import com.quasar.art.dto.RegisterDTO;
import com.quasar.art.service.impl.UserServiceImpl;
import com.quasar.art.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController // 🌟 魔法注解：告诉 Spring Boot 这是一个接收 REST 请求的类，并且自动把返回值转成 JSON
@RequestMapping("/api/users") // 统一的路由前缀，这决定了你的访问路径
@CrossOrigin(origins = "*") // 允许前端跨域请求
public class UserController {

    @Autowired
    private UserServiceImpl userService; // 把咱们写好的 Service 大管家请进来

    // ==================== 1. 登录接口 ====================
    // 访问路径: POST http://localhost:8080/api/users/login
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO loginDTO) {
        // 在最外层大门做个基础拦截，防止前端传个空包裹过来
        if (loginDTO.getAccount() == null || loginDTO.getPassword() == null) {
            return Result.error("账号和密码不能为空！");
        }
        // 直接丢给 Service 处理
        return userService.login(loginDTO);
    }

    // ==================== 2. 注册接口 ====================
    // 访问路径: POST http://localhost:8080/api/users/register
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        // 基础拦截
        if (registerDTO.getUsername() == null || registerDTO.getPassword() == null) {
            return Result.error("用户名和密码不能为空！");
        }
        // 直接丢给 Service 处理
        return userService.register(registerDTO);
    }
    
}