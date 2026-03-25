package com.quasar.art.repository.Paper;

import com.quasar.art.entity.Paper.ReviewTask;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {
// 🌟 新增：根据用户ID查历史记录，按创建时间倒序排
List<ReviewTask> findByUserIdOrderByCreatedAtDesc(Long userId);
}