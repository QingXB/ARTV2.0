package com.quasar.art.entity.Paper;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Table(name = "review_tasks")
public class ReviewTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🌟 记录是哪个用户发起的
    private Long userId;

    // 🌟 记录关联的文献 ID（存成字符串，如 "14,17,19"）
    @Column(name = "paper_ids")
    private String paperIds;

    /**
     * 🌟 状态控制：
     * 0: 等待中 (Pending)
     * 1: 正在生成 (Processing)
     * 2: 生成成功 (Success)
     * 3: 生成失败 (Failed)
     */
    private Integer status = 0;

    // 🌟 最终生成的超长 Markdown 综述存在这里
    @Column(columnDefinition = "TEXT")
    private String content;

    // 🌟 如果失败了，记录失败原因（比如：大模型超时）
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime finishedAt;
}