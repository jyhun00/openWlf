package aml.openwlf.data.service.case_;

import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.entity.CaseEntity.*;
import aml.openwlf.data.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 케이스 조회 서비스
 *
 * OOP 원칙: 단일 책임 원칙 (SRP) + CQRS 패턴
 * - 케이스 조회(Query) 작업만 담당
 * - 생성/수정은 CaseCommandService가 담당
 */
@Service
@RequiredArgsConstructor
public class CaseQueryService {

    private final CaseRepository caseRepository;

    /**
     * ID로 케이스 조회
     */
    @Transactional(readOnly = true)
    public Optional<CaseEntity> findById(Long id) {
        return caseRepository.findById(id);
    }

    /**
     * 참조번호로 케이스 조회
     */
    @Transactional(readOnly = true)
    public Optional<CaseEntity> findByReference(String reference) {
        return caseRepository.findByCaseReference(reference);
    }

    /**
     * 전체 케이스 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<CaseEntity> findAll(Pageable pageable) {
        return caseRepository.findAll(pageable);
    }

    /**
     * 열린 케이스 조회 (종료되지 않은)
     */
    @Transactional(readOnly = true)
    public Page<CaseEntity> findOpenCases(Pageable pageable) {
        List<CaseStatus> openStatuses = List.of(
                CaseStatus.OPEN, CaseStatus.IN_PROGRESS,
                CaseStatus.PENDING_INFO, CaseStatus.PENDING_REVIEW, CaseStatus.ESCALATED);
        return caseRepository.findByStatusIn(openStatuses, pageable);
    }

    /**
     * 담당자별 케이스 조회
     */
    @Transactional(readOnly = true)
    public Page<CaseEntity> findByAssignee(String assignedTo, Pageable pageable) {
        return caseRepository.findByAssignedTo(assignedTo, pageable);
    }

    /**
     * 복합 조건 검색
     */
    @Transactional(readOnly = true)
    public Page<CaseEntity> search(CaseStatus status, CasePriority priority, CaseType caseType,
                                    String assignedTo, String customerId, Pageable pageable) {
        return caseRepository.searchCases(status, priority, caseType, assignedTo, customerId, pageable);
    }

    /**
     * 고객 ID로 케이스 조회
     */
    @Transactional(readOnly = true)
    public Page<CaseEntity> findByCustomerId(String customerId, Pageable pageable) {
        return caseRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * 상태별 케이스 조회
     */
    @Transactional(readOnly = true)
    public Page<CaseEntity> findByStatus(CaseStatus status, Pageable pageable) {
        return caseRepository.findByStatus(status, pageable);
    }

    /**
     * 우선순위별 케이스 조회
     */
    @Transactional(readOnly = true)
    public Page<CaseEntity> findByPriority(CasePriority priority, Pageable pageable) {
        return caseRepository.findByPriority(priority, pageable);
    }
}
