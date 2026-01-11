package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for filtering history (audit trail)
 */
@Entity
@Table(name = "filtering_history", indexes = {
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_alert", columnList = "is_alert"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilteringHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", length = 100)
    private String customerId;
    
    @Column(name = "customer_name", nullable = false, length = 500)
    private String customerName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(length = 10)
    private String nationality;
    
    @Column(name = "is_alert", nullable = false)
    private Boolean isAlert;
    
    @Column(nullable = false)
    private Double score;
    
    @Column(name = "matched_rules", columnDefinition = "TEXT")
    private String matchedRules;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
