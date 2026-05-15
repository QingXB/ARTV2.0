package com.quasar.art.repository.Paper;
import com.quasar.art.entity.Paper.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    // 自动生成 SQL：根据用户 ID 查他上传的所有论文
    List<Paper> findByUserId(Long userId);

    // 分页查询用户文献
    Page<Paper> findByUserId(Long userId, Pageable pageable);

    // 模糊搜索标题
    Page<Paper> findByUserIdAndTitleContaining(Long userId, String keyword, Pageable pageable);

    // 按状态查询
    Page<Paper> findByUserIdAndParseStatus(Long userId, Integer parseStatus, Pageable pageable);

    // 搜索+状态筛选
    Page<Paper> findByUserIdAndTitleContainingAndParseStatus(Long userId, String keyword, Integer parseStatus, Pageable pageable);
}