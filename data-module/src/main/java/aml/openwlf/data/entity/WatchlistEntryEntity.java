package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for watchlist entries
 */
@Entity
@Table(name = "watchlist_entries", indexes = {
        @Index(name = "idx_name", columnList = "normalized_name"),
        @Index(name = "idx_source", columnList = "list_source"),
        @Index(name = "idx_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistEntryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String name;
    
    @Column(name = "normalized_name", nullable = false, length = 500)
    private String normalizedName;
    
    @Column(columnDefinition = "TEXT")
    private String aliases;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(length = 10)
    private String nationality;
    
    @Column(name = "list_source", nullable = false, length = 50)
    private String listSource;
    
    @Column(name = "entry_type", length = 50)
    private String entryType;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version_date")
    private LocalDate versionDate;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (versionDate == null) {
            versionDate = LocalDate.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
