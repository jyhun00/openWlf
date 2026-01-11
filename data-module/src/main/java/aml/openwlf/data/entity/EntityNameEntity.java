package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 이름 및 별칭 테이블
 * 
 * 검색의 핵심이므로 별도 테이블로 유지하되, 복잡한 메타데이터는 최소화합니다.
 * Trigram 인덱스를 활용한 부분 일치 검색에 최적화되어 있습니다.
 */
@Entity
@Table(name = "entity_names", indexes = {
        @Index(name = "idx_en_entity_id", columnList = "entity_id"),
        @Index(name = "idx_en_name_type", columnList = "name_type"),
        @Index(name = "idx_en_script", columnList = "script")
        // full_name에 대한 trigram 인덱스는 DDL로 별도 생성:
        // CREATE INDEX idx_en_full_name_trgm ON entity_names USING gin (full_name gin_trgm_ops);
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityNameEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "name_id")
    private Long nameId;
    
    /**
     * 부모 엔티티 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    @ToString.Exclude
    private SanctionsEntity sanctionsEntity;
    
    /**
     * 이름 유형
     * - 'Primary': 대표 이름
     * - 'AKA': Also Known As (별칭)
     * - 'FKA': Formerly Known As (이전 이름)
     * - 'Low Quality AKA': 저품질 별칭
     * - 'Spelling Variation': 철자 변형
     */
    @Column(name = "name_type", length = 50)
    private String nameType;
    
    /**
     * 전체 이름 (검색 대상)
     */
    @Column(name = "full_name", columnDefinition = "TEXT", nullable = false)
    private String fullName;
    
    /**
     * 정규화된 이름 (검색 최적화용)
     */
    @Column(name = "normalized_name", columnDefinition = "TEXT")
    private String normalizedName;
    
    /**
     * 문자 체계 ('Latin', 'Arabic', 'Cyrillic', 'Korean', 'Chinese' 등)
     */
    @Column(name = "script", length = 50)
    private String script;
    
    /**
     * 이름 품질 점수 (0-100, 높을수록 신뢰도 높음)
     */
    @Column(name = "quality_score")
    private Integer qualityScore;
    
    // ========================================
    // 개별 이름 구성요소 (선택적)
    // ========================================
    
    @Column(name = "first_name", length = 200)
    private String firstName;
    
    @Column(name = "middle_name", length = 200)
    private String middleName;
    
    @Column(name = "last_name", length = 200)
    private String lastName;
    
    /**
     * Primary 이름 여부
     */
    public boolean isPrimary() {
        return "Primary".equalsIgnoreCase(nameType);
    }
    
    /**
     * 고품질 이름 여부
     */
    public boolean isHighQuality() {
        return !"Low Quality AKA".equalsIgnoreCase(nameType);
    }
}
