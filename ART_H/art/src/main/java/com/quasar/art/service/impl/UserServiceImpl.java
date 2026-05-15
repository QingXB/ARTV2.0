package com.quasar.art.service.impl;

import com.quasar.art.dto.LoginDTO;
import com.quasar.art.dto.RegisterDTO;
import com.quasar.art.entity.User;
import com.quasar.art.repository.UserRepository;
import com.quasar.art.util.JwtUtil;
import com.quasar.art.util.PasswordUtil;
import com.quasar.art.util.Result;
import com.quasar.art.vo.LoginVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordUtil passwordUtil;

    public Result<LoginVO> login(LoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        String password = loginDTO.getPassword();

        log.info("用户尝试登录，账号: {}", account);

        User user = null;

        // 1. 智能判断账号类型
        if (account.contains("@")) {
            user = userRepository.findByEmail(account);
        } else if (account.matches("^1[3-9]\\d{9}$")) {
            user = userRepository.findByPhone(account);
        } else {
            user = userRepository.findByUsername(account);
        }

        // 2. 校验账号是否存在
        if (user == null) {
            log.warn("登录失败：账号不存在 - {}", account);
            return Result.error("账号不存在");
        }

        // 3. 校验密码（使用加密后的密码比对）
        if (!passwordUtil.matches(password, user.getPassword())) {
            log.warn("登录失败：密码错误 - {}", account);
            return Result.error("密码错误");
        }

        // 4. 登录成功，生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUsername(user.getUsername());

        log.info("用户登录成功: {} (ID: {})", user.getUsername(), user.getId());
        return Result.success(loginVO);
    }

    public Result<String> register(RegisterDTO dto) {
        log.info("收到注册请求: {}", dto.getUsername());

        // 1. 必填项基础校验
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空！");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空！");
        }

        // 密码长度校验
        if (dto.getPassword().length() < 6) {
            return Result.error("密码长度不能少于6位！");
        }

        // 2. 查重校验
        if (userRepository.existsByUsername(dto.getUsername())) {
            log.warn("注册失败：用户名已存在 - {}", dto.getUsername());
            return Result.error("该用户名已被占用，请换一个！");
        }

        // 3. 构建用户实体
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        // 使用 BCrypt 风格加密存储密码
        newUser.setPassword(passwordUtil.encode(dto.getPassword()));

        // 4. 处理邮箱
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                log.warn("注册失败：邮箱已存在 - {}", dto.getEmail());
                return Result.error("该邮箱已经被注册过了！");
            }
            newUser.setEmail(dto.getEmail());
        }

        // 5. 处理手机号
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            if (userRepository.existsByPhone(dto.getPhone())) {
                log.warn("注册失败：手机号已存在 - {}", dto.getPhone());
                return Result.error("该手机号已经被注册过了！");
            }
            newUser.setPhone(dto.getPhone());
        }

        // 6. 设置默认值
        newUser.setStatus(1);
        newUser.setAvatar("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + dto.getUsername());

        // 7. 保存入库
        userRepository.save(newUser);
        log.info("用户注册成功: {} (ID: {})", newUser.getUsername(), newUser.getId());
        return Result.success("注册成功！欢迎来到 ScholarAI！");
    }
}