package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for alerts generated from filtering
 *
 * BaseEntity를 확장하여 JPA Auditing으로 createdAt/updatedAt 자동 관리
 */
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_customer_id", columnList = "customer_id"),
        @Index(name = "idx_alert_created_at", columnList = "created_at"),
        @Index(name = "idx_alert_score", columnList = "score")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "alert_reference", nullable = false, unique = true, length = 50)
    private String alertReference;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;
    
    @Column(name = "customer_id", length = 100)
    private String customerId;
    
    @Column(name = "customer_name", nullable = false, length = 500)
    private String customerName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(length = 10)
    private String nationality;
    
    @Column(nullable = false)
    private Double score;
    
    @Column(name = "matched_rules", columnDefinition = "TEXT")
    private String matchedRules;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;
    
    @Column(name = "resolution_comment", columnDefinition = "TEXT")
    private String resolutionComment;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = AlertStatus.NEW;
        }
    }
    
    /**
     * Alert status enum
     */
    public enum AlertStatus {
        NEW,           // 신규 생성
        IN_REVIEW,     // 검토 중
        ESCALATED,     // 상위 보고
        CONFIRMED,     // 실제 위험 확인
        FALSE_POSITIVE,// 오탐
        CLOSED         // 종료
    }
}
