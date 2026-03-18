package com.quasar.art.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String account;  // 前端传来的可能是用户名、邮箱或手机号
    private String password;
}