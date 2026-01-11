package aml.openwlf.api.dto.sanctions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 제재 대상 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제재 대상 검색 조건")
public class SanctionsSearchRequest {
    
    @Schema(description = "이름 검색어", example = "kim")
    private String name;
    
    @Schema(description = "국적 필터", example = "KP")
    private String nationality;
    
    @Schema(description = "데이터 출처 필터", example = "UN")
    private String sourceFile;
    
    @Schema(description = "엔티티 유형 필터", example = "Individual")
    private String entityType;
    
    @Schema(description = "제재 리스트 유형 필터", example = "SDN")
    private String sanctionListType;
}
