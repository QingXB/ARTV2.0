package com.quasar.art.entity.Paper;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "literature_reviews")
public class LiteratureReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "outline_content", columnDefinition = "TEXT")
    private String outlineContent;

    // 🌟 高级技巧：直接将 JSONB 数组映射为 Java 的 List<Long>
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_paper_ids", columnDefinition = "jsonb")
    private List<Long> selectedPaperIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}