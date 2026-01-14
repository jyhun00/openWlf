package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 제재 대상 마스터 테이블 (비정규화 버전)
 * 
 * 자주 검색되는 필드(국적, 생년월일, 성별 등)를 컬럼으로 전진 배치하고,
 * 가변적인 추가 정보는 JSONB로 통합하여 조인을 최소화합니다.
 */
@Entity
@Table(name = "sanctions_entities", indexes = {
        @Index(name = "idx_se_source_uid", columnList = "source_uid"),
        @Index(name = "idx_se_source_file", columnList = "source_file"),
        @Index(name = "idx_se_entity_type", columnList = "entity_type"),
        @Index(name = "idx_se_nationality", columnList = "nationality"),
        @Index(name = "idx_se_birth_date", columnList = "birth_date"),
        @Index(name = "idx_se_sanction_list_type", columnList = "sanction_list_type"),
        @Index(name = "idx_se_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanctionsEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entity_id")
    private Long entityId;
    
    /**
     * 원본 데이터의 고유 ID (UN: DATAID, OFAC: UID 등)
     */
    @Column(name = "source_uid", length = 100)
    private String sourceUid;
    
    /**
     * 데이터 출처 ('UN', 'OFAC', 'EU' 등)
     */
    @Column(name = "source_file", length = 50)
    private String sourceFile;
    
    /**
     * 엔티티 유형 ('Individual', 'Entity', 'Vessel')
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    // ========================================
    // 주요 검색 필드 (정규화 완화 - 컬럼으로 전진 배치)
    // ========================================
    
    /**
     * 대표 이름 (Primary Name)
     */
    @Column(name = "primary_name", columnDefinition = "TEXT")
    private String primaryName;
    
    /**
     * 정규화된 이름 (검색용)
     */
    @Column(name = "normalized_name", columnDefinition = "TEXT")
    private String normalizedName;
    
    /**
     * 성별 ('Male', 'Female')
     */
    @Column(name = "gender", length = 20)
    private String gender;
    
    /**
     * 생년월일
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    /**
     * 국적 (복수 국적은 콤마로 구분 또는 additional_features에 배열로)
     */
    @Column(name = "nationality", length = 100)
    private String nationality;
    
    /**
     * 선박 국적 (Vessel인 경우)
     */
    @Column(name = "vessel_flag", length = 100)
    private String vesselFlag;
    
    // ========================================
    // 가변 속성 (JSONB)
    // ========================================
    
    /**
     * 기타 가변적인 모든 데이터를 담는 JSONB 컬럼
     * 
     * 예시:
     * {
     *   "placeOfBirth": "Pyongyang",
     *   "titles": ["General", "Minister"],
     *   "nationalities": ["KP", "RU"],
     *   "designations": ["UN Resolution 1234"],
     *   "comments": "Additional information...",
     *   "programs": ["DPRK", "IRAN"],
     *   "idNumbers": [{"type": "Passport", "number": "123456"}]
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_features", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> additionalFeatures = new HashMap<>();
    
    /**
     * 제재 리스트 유형 ('SDN', 'Consolidated List' 등)
     */
    @Column(name = "sanction_list_type", length = 100)
    private String sanctionListType;
    
    /**
     * 활성 상태
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * 마지막 업데이트 시간
     */
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
    
    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // ========================================
    // 연관 관계 (이름/별칭은 별도 테이블)
    // ========================================
    
    @OneToMany(mappedBy = "sanctionsEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EntityNameEntity> names = new ArrayList<>();
    
    @OneToMany(mappedBy = "sanctionsEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EntityAddressEntity> addresses = new ArrayList<>();
    
    @OneToMany(mappedBy = "sanctionsEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EntityDocumentEntity> documents = new ArrayList<>();
    
    // ========================================
    // Lifecycle Callbacks
    // ========================================
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
    
    // ========================================
    // Helper Methods
    // ========================================
    
    public void addName(EntityNameEntity name) {
        names.add(name);
        name.setSanctionsEntity(this);
    }
    
    public void removeName(EntityNameEntity name) {
        names.remove(name);
        name.setSanctionsEntity(null);
    }
    
    public void addAddress(EntityAddressEntity address) {
        addresses.add(address);
        address.setSanctionsEntity(this);
    }
    
    public void addDocument(EntityDocumentEntity document) {
        documents.add(document);
        document.setSanctionsEntity(this);
    }
    
    /**
     * additional_features에 값 추가
     */
    public void addFeature(String key, Object value) {
        if (additionalFeatures == null) {
            additionalFeatures = new HashMap<>();
        }
        additionalFeatures.put(key, value);
    }
    
    /**
     * additional_features에서 값 조회
     */
    public <T> T getFeature(String key, Class<T> type) {
        if (additionalFeatures == null) {
            return null;
        }
        Object value = additionalFeatures.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }
}
