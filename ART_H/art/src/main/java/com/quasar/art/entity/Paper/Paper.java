package com.quasar.art.entity.Paper;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "papers")
public class Paper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    // 状态: 0未解析, 1解析中, 2已解析, 3解析失败
    @Column(name = "parse_status", columnDefinition = "int default 0")
    private Integer parseStatus = 0; 

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}