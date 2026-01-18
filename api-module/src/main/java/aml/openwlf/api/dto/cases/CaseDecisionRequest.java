package aml.openwlf.api.dto.cases;

import aml.openwlf.api.validation.ValidEnum;
import aml.openwlf.data.entity.CaseEntity.CaseDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Case 최종 결정 요청 DTO
 *
 * OOP 원칙: 입력 검증을 통한 캡슐화
 * - Bean Validation으로 유효하지 않은 데이터 차단
 * - @ValidEnum으로 타입 안전성 확보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 최종 결정 요청")
public class CaseDecisionRequest {

    @NotBlank(message = "결정은 필수입니다")
    @ValidEnum(enumClass = CaseDecision.class, nullable = false,
            message = "유효하지 않은 결정입니다")
    @Schema(description = "최종 결정", example = "TRUE_POSITIVE", required = true,
            allowableValues = {"TRUE_POSITIVE", "FALSE_POSITIVE", "INCONCLUSIVE",
                    "ESCALATED_TO_LE", "NO_ACTION_REQUIRED"})
    private String decision;

    @NotBlank(message = "결정 사유는 필수입니다")
    @Size(min = 10, max = 2000, message = "결정 사유는 10자 이상 2000자 이하여야 합니다")
    @Schema(description = "결정 사유",
            example = "Customer name matches OFAC SDN list entry with 95% confidence. " +
                    "DOB and nationality also match.", required = true)
    private String rationale;

    @NotBlank(message = "결정자 정보는 필수입니다")
    @Size(max = 100, message = "결정자 ID는 100자를 초과할 수 없습니다")
    @Schema(description = "결정자 ID", example = "senior_analyst", required = true)
    private String decidedBy;
}
