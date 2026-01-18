package aml.openwlf.api.dto.cases;

import aml.openwlf.api.validation.ValidEnum;
import aml.openwlf.data.entity.CaseEntity.CaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alert에서 Case 생성 요청 DTO
 *
 * OOP 원칙: 입력 검증을 통한 캡슐화
 * - Bean Validation으로 유효하지 않은 데이터 차단
 * - @ValidEnum으로 타입 안전성 확보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert에서 Case 생성 요청")
public class CreateCaseFromAlertRequest {

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    @Schema(description = "케이스 제목 (미입력시 자동 생성)",
            example = "Investigation for suspicious sanctions match")
    private String title;

    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다")
    @Schema(description = "케이스 설명")
    private String description;

    @ValidEnum(enumClass = CaseType.class, nullable = true,
            message = "유효하지 않은 케이스 유형입니다")
    @Schema(description = "케이스 유형", example = "SANCTIONS",
            allowableValues = {"SANCTIONS", "PEP", "ADVERSE_MEDIA", "FRAUD",
                    "MONEY_LAUNDERING", "TERRORIST_FINANCING", "OTHER"})
    private String caseType;

    @Size(max = 100, message = "담당자 ID는 100자를 초과할 수 없습니다")
    @Schema(description = "담당자 ID", example = "analyst1")
    private String assignedTo;

    @Size(max = 100, message = "담당 팀은 100자를 초과할 수 없습니다")
    @Schema(description = "담당 팀", example = "AML Investigation Team")
    private String assignedTeam;

    @NotBlank(message = "생성자 정보는 필수입니다")
    @Size(max = 100, message = "생성자 ID는 100자를 초과할 수 없습니다")
    @Schema(description = "생성자 ID", example = "admin", required = true)
    private String createdBy;
}
