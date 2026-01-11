package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 주소 정보 테이블
 * 
 * 주소 데이터는 양이 많고 검색 빈도는 상대적으로 낮으므로
 * 별도 테이블로 유지합니다.
 */
@Entity
@Table(name = "entity_addresses", indexes = {
        @Index(name = "idx_ea_entity_id", columnList = "entity_id"),
        @Index(name = "idx_ea_country", columnList = "country"),
        @Index(name = "idx_ea_city", columnList = "city")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityAddressEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;
    
    /**
     * 부모 엔티티 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    @ToString.Exclude
    private SanctionsEntity sanctionsEntity;
    
    /**
     * 주소 유형 ('Registered', 'Residential', 'Business' 등)
     */
    @Column(name = "address_type", length = 50)
    private String addressType;
    
    /**
     * 전체 주소 (한 줄로 통합)
     */
    @Column(name = "full_address", columnDefinition = "TEXT")
    private String fullAddress;
    
    /**
     * 상세 주소 (거리, 건물명 등)
     */
    @Column(name = "street", columnDefinition = "TEXT")
    private String street;
    
    /**
     * 도시
     */
    @Column(name = "city", length = 100)
    private String city;
    
    /**
     * 주/도/지역
     */
    @Column(name = "state_province", length = 100)
    private String stateProvince;
    
    /**
     * 우편번호
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    /**
     * 국가 (ISO 코드 또는 전체명)
     */
    @Column(name = "country", length = 100)
    private String country;
    
    /**
     * 국가 ISO 코드 (2자리)
     */
    @Column(name = "country_code", length = 10)
    private String countryCode;
    
    /**
     * 비고
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
