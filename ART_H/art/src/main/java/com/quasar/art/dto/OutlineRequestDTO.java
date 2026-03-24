package com.quasar.art.dto;

import lombok.Data;
import java.util.List;

@Data
public class OutlineRequestDTO {
    // 接收 Vue 传过来的勾选的文献 ID 列表
    private List<Long> paperIds; 
}