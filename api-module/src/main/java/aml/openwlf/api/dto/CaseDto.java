package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Case DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "케이스 정보")
public class CaseDto {
    
    @Schema(description = "케이스 ID", example = "1")
    private Long id;
    
    @Schema(description = "케이스 참조번호", example = "CASE-20250111-A1B2C3")
    private String caseReference;
    
    @Schema(description = "케이스 제목", example = "Sanctions match investigation for John Smith")
    private String title;
    
    @Schema(description = "케이스 설명")
    private String description;
    
    @Schema(description = "케이스 상태", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "우선순위", example = "HIGH")
    private String priority;
    
    @Schema(description = "케이스 유형", example = "SANCTIONS")
    private String caseType;
    
    @Schema(description = "고객 ID", example = "CUST-12345")
    private String customerId;
    
    @Schema(description = "고객명", example = "John Smith")
    private String customerName;
    
    @Schema(description = "생년월일", example = "1975-05-15")
    private LocalDate dateOfBirth;
    
    @Schema(description = "국적", example = "US")
    private String nationality;
    
    @Schema(description = "리스크 점수", example = "85.0")
    private Double riskScore;
    
    @Schema(description = "담당자", example = "analyst1")
    private String assignedTo;
    
    @Schema(description = "담당 팀", example = "AML Team")
    private String assignedTeam;
    
    @Schema(description = "마감 기한")
    private LocalDateTime dueDate;
    
    @Schema(description = "최종 결정", example = "TRUE_POSITIVE")
    private String decision;
    
    @Schema(description = "결정 사유")
    private String decisionRationale;
    
    @Schema(description = "SAR 제출 여부")
    private Boolean sarFiled;
    
    @Schema(description = "SAR 참조번호")
    private String sarReference;
    
    @Schema(description = "종료일시")
    private LocalDateTime closedAt;
    
    @Schema(description = "종료자")
    private String closedBy;
    
    @Schema(description = "생성자")
    private String createdBy;
    
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;
    
    @Schema(description = "연결된 Alert 수")
    private Integer linkedAlertCount;
    
    @Schema(description = "코멘트 수")
    private Integer commentCount;
}
