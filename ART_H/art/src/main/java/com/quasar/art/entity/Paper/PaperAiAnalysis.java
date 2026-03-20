package com.quasar.art.entity.Paper;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "paper_ai_analyses")
public class PaperAiAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paper_id", nullable = false, unique = true)
    private Long paperId;

    @Column(name = "research_question", columnDefinition = "TEXT")
    private String researchQuestion;

    @Column(columnDefinition = "TEXT")
    private String methodology;

    @Column(columnDefinition = "TEXT")
    private String conclusion;

    // 🌟 高级技巧：直接将 PostgreSQL 的 JSONB 映射为 Java 的 String
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ai_response", columnDefinition = "jsonb")
    private String rawAiResponse;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}