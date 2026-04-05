package com.quasar.art.entity.enums;

/**
 * 文献关系类型枚举
 */
public enum RelationType {
    INHERIT("传承", "后续研究在前人基础上发展"),
    CONTRADICT("矛盾", "研究结论相互矛盾"),
    SUPPORT("支持", "多个研究结论相互支持");

    private final String description;
    private final String meaning;

    RelationType(String description, String meaning) {
        this.description = description;
        this.meaning = meaning;
    }

    public String getDescription() {
        return description;
    }

    public String getMeaning() {
        return meaning;
    }
}
