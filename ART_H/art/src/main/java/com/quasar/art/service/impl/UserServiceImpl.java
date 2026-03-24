package com.quasar.art.service.impl;

import com.quasar.art.dto.LoginDTO;
import com.quasar.art.dto.RegisterDTO;
import com.quasar.art.entity.User;
import com.quasar.art.repository.UserRepository;
import com.quasar.art.util.Result;
import com.quasar.art.vo.LoginVO;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl {

    @Autowired
    private UserRepository userRepository;

    // ==================== 1. 登录业务 ====================
    public Result<LoginVO> login(LoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        String password = loginDTO.getPassword();
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
            return Result.error("账号不存在");
        }

        // 3. 校验密码 
        if (!user.getPassword().equals(password)) {
            return Result.error("密码错误");
        }

        // 4. 登录成功，生成 Token
       
        String mockToken = "quasar-auth-token-" + user.getId();
        String userName = user.getUsername();
        // 组装专门给前端的 VO
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(mockToken);
        loginVO.setUsername(userName); // 🌟 改成 setUsername
        return Result.success(loginVO);
    }

    // ==================== 2. 注册业务 (接在登录下面) ====================
    public Result<String> register(RegisterDTO dto) {
        // 1. 必填项基础校验
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空！");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空！");
        }

        // 2. 查重校验 (依赖 UserRepository 里的 existsByXXX 方法)
        if (userRepository.existsByUsername(dto.getUsername())) {
            return Result.error("该用户名已被占用，请换一个！");
        }

        // 3. 构建用户实体
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setPassword(dto.getPassword()); // 暂存明文

        // 4. 处理选填项 (邮箱) - 必须把空字符串转成 null，防止触发 UNIQUE 报错
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                return Result.error("该邮箱已经被注册过了！");
            }
            newUser.setEmail(dto.getEmail());
        } else {
            newUser.setEmail(null); 
        }

        // 5. 处理选填项 (手机号)
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            if (userRepository.existsByPhone(dto.getPhone())) {
                return Result.error("该手机号已经被注册过了！");
            }
            newUser.setPhone(dto.getPhone());
        } else {
            newUser.setPhone(null);
        }

        // 6. 设置默认值 (状态与预留头像)
        newUser.setStatus((Integer) 1); // 状态正常
        newUser.setAvatar("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + dto.getUsername());

        // 7. 保存入库
        userRepository.save(newUser);
        return Result.success("注册成功！欢迎来到 ScholarAI！");
    }
}