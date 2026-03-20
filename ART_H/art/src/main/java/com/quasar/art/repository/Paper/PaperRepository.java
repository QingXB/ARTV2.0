package com.quasar.art.repository.Paper;
import com.quasar.art.entity.Paper.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    // 自动生成 SQL：根据用户 ID 查他上传的所有论文
    List<Paper> findByUserId(Long userId);
}