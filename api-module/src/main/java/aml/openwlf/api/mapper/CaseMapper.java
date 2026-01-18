package aml.openwlf.api.mapper;

import aml.openwlf.api.dto.CaseDto;
import aml.openwlf.api.dto.cases.CaseActivityDto;
import aml.openwlf.api.dto.cases.CaseCommentDto;
import aml.openwlf.data.entity.CaseActivityEntity;
import aml.openwlf.data.entity.CaseCommentEntity;
import aml.openwlf.data.entity.CaseEntity;
import aml.openwlf.data.service.case_.CaseAlertLinkService;
import aml.openwlf.data.service.case_.CaseCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Case 관련 Entity를 DTO로 변환하는 매퍼
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - Entity ↔ DTO 변환 로직만 담당
 * - Controller의 책임을 분리하여 테스트 용이성 향상
 */
@Component
@RequiredArgsConstructor
public class CaseMapper {

    private final CaseAlertLinkService caseAlertLinkService;
    private final CaseCommentService caseCommentService;

    /**
     * CaseEntity → CaseDto 변환
     */
    public CaseDto toDto(CaseEntity entity) {
        return CaseDto.builder()
                .id(entity.getId())
                .caseReference(entity.getCaseReference())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus().name())
                .priority(entity.getPriority().name())
                .caseType(entity.getCaseType().name())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .riskScore(entity.getRiskScore())
                .assignedTo(entity.getAssignedTo())
                .assignedTeam(entity.getAssignedTeam())
                .dueDate(entity.getDueDate())
                .decision(entity.getDecision() != null ? entity.getDecision().name() : null)
                .decisionRationale(entity.getDecisionRationale())
                .sarFiled(entity.getSarFiled())
                .sarReference(entity.getSarReference())
                .closedAt(entity.getClosedAt())
                .closedBy(entity.getClosedBy())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .linkedAlertCount((int) caseAlertLinkService.getLinkedAlertCount(entity.getId()))
                .commentCount((int) caseCommentService.getCommentCount(entity.getId()))
                .build();
    }

    /**
     * CaseEntity → CaseDto 변환 (연관 개수 없이)
     * 성능이 중요한 목록 조회 시 사용
     */
    public CaseDto toDtoWithoutCounts(CaseEntity entity) {
        return CaseDto.builder()
                .id(entity.getId())
                .caseReference(entity.getCaseReference())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus().name())
                .priority(entity.getPriority().name())
                .caseType(entity.getCaseType().name())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .riskScore(entity.getRiskScore())
                .assignedTo(entity.getAssignedTo())
                .assignedTeam(entity.getAssignedTeam())
                .dueDate(entity.getDueDate())
                .decision(entity.getDecision() != null ? entity.getDecision().name() : null)
                .decisionRationale(entity.getDecisionRationale())
                .sarFiled(entity.getSarFiled())
                .sarReference(entity.getSarReference())
                .closedAt(entity.getClosedAt())
                .closedBy(entity.getClosedBy())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * CaseCommentEntity → CaseCommentDto 변환
     */
    public CaseCommentDto toCommentDto(CaseCommentEntity entity) {
        return CaseCommentDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseEntity().getId())
                .content(entity.getContent())
                .commentType(entity.getCommentType().name())
                .isInternal(entity.getIsInternal())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * CaseCommentEntity 목록 → CaseCommentDto 목록 변환
     */
    public List<CaseCommentDto> toCommentDtoList(List<CaseCommentEntity> entities) {
        return entities.stream()
                .map(this::toCommentDto)
                .toList();
    }

    /**
     * CaseActivityEntity → CaseActivityDto 변환
     */
    public CaseActivityDto toActivityDto(CaseActivityEntity entity) {
        return CaseActivityDto.builder()
                .id(entity.getId())
                .caseId(entity.getCaseEntity().getId())
                .activityType(entity.getActivityType().name())
                .description(entity.getDescription())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .performedBy(entity.getPerformedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * CaseActivityEntity 목록 → CaseActivityDto 목록 변환
     */
    public List<CaseActivityDto> toActivityDtoList(List<CaseActivityEntity> entities) {
        return entities.stream()
                .map(this::toActivityDto)
                .toList();
    }
}
