package aml.openwlf.api.dto.sanctions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 제재 대상 목록 조회용 간략 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제재 대상 목록 아이템")
public class SanctionsListItemDto {
    
    @Schema(description = "엔티티 ID", example = "1")
    private Long entityId;
    
    @Schema(description = "원본 UID", example = "UN-6908330")
    private String sourceUid;
    
    @Schema(description = "데이터 출처", example = "UN")
    private String sourceFile;
    
    @Schema(description = "엔티티 유형", example = "Individual")
    private String entityType;
    
    @Schema(description = "대표 이름", example = "KIM JONG UN")
    private String primaryName;
    
    @Schema(description = "별칭 (AKA) 목록")
    private List<String> aliases;
    
    @Schema(description = "국적", example = "KP")
    private String nationality;
    
    @Schema(description = "생년월일", example = "1984-01-08")
    private LocalDate birthDate;
    
    @Schema(description = "제재 리스트 유형", example = "UN Security Council Consolidated List")
    private String sanctionListType;
    
    @Schema(description = "마지막 업데이트", example = "2025-12-28T15:30:00")
    private LocalDateTime lastUpdatedAt;
}
