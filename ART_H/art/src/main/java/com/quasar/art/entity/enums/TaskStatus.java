package com.quasar.art.entity.enums;

/**
 * 综述任务状态枚举
 */
public enum TaskStatus {
    PENDING(0, "等待中"),
    PROCESSING(1, "生成中"),
    SUCCESS(2, "生成成功"),
    FAILED(3, "生成失败");

    private final int code;
    private final String description;

    TaskStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的状态码: " + code);
    }
}
