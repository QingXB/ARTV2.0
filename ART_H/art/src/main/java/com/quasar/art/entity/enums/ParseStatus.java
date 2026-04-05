package com.quasar.art.entity.enums;

/**
 * 文献解析状态枚举
 */
public enum ParseStatus {
    PENDING(0, "待解析"),
    PROCESSING(1, "解析中"),
    SUCCESS(2, "已解析"),
    FAILED(3, "解析失败");

    private final int code;
    private final String description;

    ParseStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParseStatus fromCode(int code) {
        for (ParseStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的状态码: " + code);
    }
}
