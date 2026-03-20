package com.quasar.art.repository.Paper;
import com.quasar.art.entity.Paper.PaperAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperAiAnalysisRepository extends JpaRepository<PaperAiAnalysis, Long> {
    // 自动生成 SQL：根据论文 ID 找对应的 AI 解析结果
    PaperAiAnalysis findByPaperId(Long paperId);
}