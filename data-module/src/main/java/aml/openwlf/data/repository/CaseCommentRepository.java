package aml.openwlf.data.repository;

import aml.openwlf.data.entity.CaseCommentEntity;
import aml.openwlf.data.entity.CaseCommentEntity.CommentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseCommentRepository extends JpaRepository<CaseCommentEntity, Long> {
    
    /**
     * 케이스의 코멘트 목록 (페이징)
     */
    Page<CaseCommentEntity> findByCaseEntityIdOrderByCreatedAtDesc(Long caseId, Pageable pageable);
    
    /**
     * 케이스의 모든 코멘트 목록
     */
    List<CaseCommentEntity> findByCaseEntityIdOrderByCreatedAtDesc(Long caseId);
    
    /**
     * 케이스의 특정 유형 코멘트
     */
    List<CaseCommentEntity> findByCaseEntityIdAndCommentTypeOrderByCreatedAtDesc(
            Long caseId, CommentType commentType);
    
    /**
     * 케이스의 코멘트 수
     */
    long countByCaseEntityId(Long caseId);
}
