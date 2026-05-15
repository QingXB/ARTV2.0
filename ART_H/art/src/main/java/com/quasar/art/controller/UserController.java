package com.quasar.art.controller;

import com.quasar.art.dto.LoginDTO;
import com.quasar.art.dto.RegisterDTO;
import com.quasar.art.service.impl.UserServiceImpl;
import com.quasar.art.util.Result;
import com.quasar.art.vo.LoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        if (loginDTO.getAccount() == null || loginDTO.getAccount().trim().isEmpty()) {
            return Result.error("账号不能为空！");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空！");
        }
        log.info("收到登录请求: {}", loginDTO.getAccount());
        return userService.login(loginDTO);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空！");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空！");
        }
        log.info("收到注册请求: {}", registerDTO.getUsername());
        return userService.register(registerDTO);
    }
}