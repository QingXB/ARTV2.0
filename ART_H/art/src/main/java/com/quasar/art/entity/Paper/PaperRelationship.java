package com.quasar.art.entity.Paper;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "paper_relationships")
public class PaperRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_paper_id", nullable = false)
    private Long sourcePaperId;

    @Column(name = "target_paper_id", nullable = false)
    private Long targetPaperId;

    // INHERIT, CONTRADICT, SUPPORT
    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}