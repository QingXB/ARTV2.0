package com.quasar.art.util;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 200表示成功，400表示失败
    private String message;
    private T data; // 返回的具体数据，比如 Token

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMessage(message);
        return result;
    }
}