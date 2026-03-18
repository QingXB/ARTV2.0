package com.quasar.art.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    // 必填项
    private String username; 
    private String password; 
    
    // 选填项
    private String email; 
    private String phone; 
}