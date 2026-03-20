package com.quasar.art.repository.Paper;
import com.quasar.art.entity.Paper.LiteratureReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LiteratureReviewRepository extends JpaRepository<LiteratureReview, Long> {
    // 查某个用户生成过的所有文献综述
    List<LiteratureReview> findByUserId(Long userId);
}