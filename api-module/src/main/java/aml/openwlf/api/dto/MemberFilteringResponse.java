package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for member registration filtering result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Member registration watchlist filtering result")
public class MemberFilteringResponse {

    @Schema(description = "Alert flag indicating if member triggered a watchlist match", example = "true")
    private boolean alert;

    @Schema(description = "Risk score (0-100)", example = "85.5")
    private double score;

    @Schema(description = "List of matched rules")
    private List<MatchedRuleDto> matchedRules;

    @Schema(description = "Whether registration is allowed", example = "false")
    private boolean registrationAllowed;

    @Schema(description = "Rejection reason if registration is not allowed",
            example = "Watchlist 필터링 점수(85.5)가 임계값(70.0)을 초과했습니다.")
    private String rejectionReason;

    @Schema(description = "Alert reference number (generated when alert is created)",
            example = "ALT-20260207-A1B2C3D4")
    private String alertReference;

    @Schema(description = "Member information that was filtered")
    private MemberInfoDto memberInfo;
}
