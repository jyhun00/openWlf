package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Case-Alert 연결 엔티티
 * 하나의 Case에 여러 Alert를 연결합니다.
 */
@Entity
@Table(name = "case_alerts", indexes = {
        @Index(name = "idx_case_alert_case_id", columnList = "case_id"),
        @Index(name = "idx_case_alert_alert_id", columnList = "alert_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_case_alert", columnNames = {"case_id", "alert_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAlertEntity {
    
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
     * 연결된 Alert
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private AlertEntity alertEntity;
    
    /**
     * 연결 사유
     */
    @Column(name = "link_reason", length = 500)
    private String linkReason;
    
    /**
     * 연결자
     */
    @Column(name = "linked_by", length = 100)
    private String linkedBy;
    
    /**
     * 연결 일시
     */
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
    
    @PrePersist
    protected void onCreate() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}
