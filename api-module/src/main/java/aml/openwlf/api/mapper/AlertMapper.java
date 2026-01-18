package aml.openwlf.api.mapper;

import aml.openwlf.api.dto.AlertDto;
import aml.openwlf.data.entity.AlertEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Alert 관련 Entity를 DTO로 변환하는 매퍼
 *
 * OOP 원칙: 단일 책임 원칙 (SRP)
 * - Entity ↔ DTO 변환 로직만 담당
 */
@Component
public class AlertMapper {

    /**
     * AlertEntity → AlertDto 변환
     */
    public AlertDto toDto(AlertEntity entity) {
        return AlertDto.builder()
                .id(entity.getId())
                .alertReference(entity.getAlertReference())
                .status(entity.getStatus().name())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .dateOfBirth(entity.getDateOfBirth())
                .nationality(entity.getNationality())
                .score(entity.getScore())
                .explanation(entity.getExplanation())
                .assignedTo(entity.getAssignedTo())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * AlertEntity 목록 → AlertDto 목록 변환
     */
    public List<AlertDto> toDtoList(List<AlertEntity> entities) {
        return entities.stream()
                .map(this::toDto)
                .toList();
    }
}
