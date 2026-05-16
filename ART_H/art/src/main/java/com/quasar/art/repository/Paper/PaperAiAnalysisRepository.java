package com.quasar.art.repository.Paper;
import com.quasar.art.entity.Paper.PaperAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperAiAnalysisRepository extends JpaRepository<PaperAiAnalysis, Long> {
    // 自动生成 SQL：根据论文 ID 找对应的 AI 解析结果
    PaperAiAnalysis findByPaperId(Long paperId);
    
    // 根据论文 ID 列表批量查询 AI 解析结果
    List<PaperAiAnalysis> findByPaperIdIn(List<Long> paperIds);
}