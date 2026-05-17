package com.quasar.art.repository.Paper;

import com.quasar.art.entity.Paper.PaperEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaperEmbeddingRepository extends JpaRepository<PaperEmbedding, Long> {
    
    Optional<PaperEmbedding> findByPaperId(Long paperId);
    
    List<PaperEmbedding> findByPaperIdIn(List<Long> paperIds);
    
    boolean existsByPaperId(Long paperId);
    
    void deleteByPaperId(Long paperId);
    
    void deleteByPaperIdIn(List<Long> paperIds);
}