package com.quasar.art.entity;

import jakarta.persistence.*; // 如果你用的是 Spring Boot 2.x，请改成 javax.persistence.*
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users") // 明确指定对应数据库里的 users 表
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 对应 PostgreSQL 的 SERIAL 自增主键
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String password;

    private String avatar;

    @Column(columnDefinition = "smallint default 1")
    private Integer status;

    // 🌟 神器：插入数据时，Hibernate 会自动填入当前时间，不需要你手动 setCreatedAt
    @CreationTimestamp 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 🌟 神器：每次更新这条数据时，时间会自动刷新
    @UpdateTimestamp 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}