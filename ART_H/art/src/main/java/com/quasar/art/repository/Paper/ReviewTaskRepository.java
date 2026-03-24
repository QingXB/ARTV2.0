package com.quasar.art.repository.Paper;

import com.quasar.art.entity.Paper.ReviewTask;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {
    // 🌟 数据隔离：只查当前用户的任务，并按创建时间倒序排
    List<ReviewTask> findByUserIdOrderByCreatedAtDesc(Long userId);
}