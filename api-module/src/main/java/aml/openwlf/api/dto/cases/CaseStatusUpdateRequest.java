package aml.openwlf.api.dto.cases;

import aml.openwlf.api.validation.ValidEnum;
import aml.openwlf.data.entity.CaseEntity.CaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Case 상태 변경 요청 DTO
 *
 * OOP 원칙: 입력 검증을 통한 캡슐화
 * - Bean Validation으로 유효하지 않은 데이터 차단
 * - @ValidEnum으로 타입 안전성 확보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 상태 변경 요청")
public class CaseStatusUpdateRequest {

    @NotBlank(message = "상태는 필수입니다")
    @ValidEnum(enumClass = CaseStatus.class, nullable = false,
            message = "유효하지 않은 상태입니다")
    @Schema(description = "새로운 상태", example = "IN_PROGRESS", required = true,
            allowableValues = {"OPEN", "IN_PROGRESS", "PENDING_INFO",
                    "PENDING_REVIEW", "ESCALATED", "CLOSED"})
    private String status;

    @NotBlank(message = "수정자 정보는 필수입니다")
    @Size(max = 100, message = "수정자 ID는 100자를 초과할 수 없습니다")
    @Schema(description = "수정자 ID", example = "analyst1", required = true)
    private String updatedBy;
}
