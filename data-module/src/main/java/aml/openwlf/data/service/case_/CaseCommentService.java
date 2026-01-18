package aml.openwlf.data.service.case_;

import aml.openwlf.data.entity.CaseCommentEntity;
import aml.openwlf.data.entity.CaseCommentEntity.CommentType;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.exception.EntityNotFoundException;
import aml.openwlf.data.repository.CaseCommentRepository;
import aml.openwlf.data.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 케이스 코멘트 서비스
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - 케이스 코멘트 생성 및 조회만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseCommentService {

    private final CaseRepository caseRepository;
    private final CaseCommentRepository caseCommentRepository;
    private final CaseActivityService caseActivityService;

    /**
     * 코멘트 추가
     */
    @Transactional
    public CaseCommentEntity addComment(Long caseId, String content,
                                         CommentType type, String createdBy) {
        CaseEntity caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> EntityNotFoundException.caseEntity(caseId));

        CaseCommentEntity comment = CaseCommentEntity.builder()
                .caseEntity(caseEntity)
                .content(content)
                .commentType(type)
                .createdBy(createdBy)
                .build();

        CaseCommentEntity saved = caseCommentRepository.save(comment);

        // 활동 로그
        caseActivityService.logCommentAdded(caseEntity, type.name(), createdBy);

        log.info("Comment added to case {} by {}", caseEntity.getCaseReference(), createdBy);
        return saved;
    }

    /**
     * 결정 코멘트 추가 (내부용)
     */
    @Transactional
    public CaseCommentEntity addDecisionComment(CaseEntity caseEntity, String rationale, String decidedBy) {
        CaseCommentEntity comment = CaseCommentEntity.builder()
                .caseEntity(caseEntity)
                .content(rationale)
                .commentType(CommentType.DECISION)
                .createdBy(decidedBy)
                .build();

        return caseCommentRepository.save(comment);
    }

    /**
     * 코멘트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CaseCommentEntity> getComments(Long caseId) {
        return caseCommentRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
    }

    /**
     * 코멘트 수 조회
     */
    @Transactional(readOnly = true)
    public long getCommentCount(Long caseId) {
        return caseCommentRepository.countByCaseEntityId(caseId);
    }
}
