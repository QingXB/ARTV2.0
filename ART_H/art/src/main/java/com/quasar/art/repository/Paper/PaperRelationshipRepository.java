package com.quasar.art.repository.Paper;
import com.quasar.art.entity.Paper.PaperRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperRelationshipRepository extends JpaRepository<PaperRelationship, Long> {
    // 找出一篇论文所有“指向别人”和“被别人指向”的关系（画图谱用）
    List<PaperRelationship> findBySourcePaperIdOrTargetPaperId(Long sourceId, Long targetId);
}