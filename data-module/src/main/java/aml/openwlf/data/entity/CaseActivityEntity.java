package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 케이스 활동 로그 엔티티
 * 케이스에 대한 모든 활동을 기록하여 감사 추적에 사용합니다.
 */
@Entity
@Table(name = "case_activities", indexes = {
        @Index(name = "idx_activity_case_id", columnList = "case_id"),
        @Index(name = "idx_activity_type", columnList = "activity_type"),
        @Index(name = "idx_activity_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseActivityEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 연결된 케이스
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseEntity caseEntity;
    
    /**
     * 활동 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;
    
    /**
     * 활동 설명
     */
    @Column(nullable = false, length = 1000)
    private String description;
    
    /**
     * 이전 값 (상태 변경 등에서 사용)
     */
    @Column(name = "old_value", length = 500)
    private String oldValue;
    
    /**
     * 새 값 (상태 변경 등에서 사용)
     */
    @Column(name = "new_value", length = 500)
    private String newValue;
    
    /**
     * 수행자
     */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;
    
    /**
     * 수행 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    /**
     * 활동 유형
     */
    public enum ActivityType {
        // 케이스 생성/수정
        CASE_CREATED,
        CASE_UPDATED,
        CASE_CLOSED,
        CASE_REOPENED,
        
        // 상태 변경
        STATUS_CHANGED,
        PRIORITY_CHANGED,
        
        // 담당자 변경
        ASSIGNED,
        REASSIGNED,
        UNASSIGNED,
        
        // Alert 연결
        ALERT_LINKED,
        ALERT_UNLINKED,
        
        // 코멘트
        COMMENT_ADDED,
        COMMENT_UPDATED,
        COMMENT_DELETED,
        
        // 결정
        DECISION_MADE,
        DECISION_CHANGED,
        
        // SAR
        SAR_FILED,
        SAR_UPDATED,
        
        // 에스컬레이션
        ESCALATED,
        DE_ESCALATED,
        
        // 기타
        DOCUMENT_ATTACHED,
        DOCUMENT_REMOVED,
        DUE_DATE_CHANGED,
        OTHER
    }
}
