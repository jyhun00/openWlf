package aml.openwlf.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 문서/신분증 정보 테이블
 * 
 * 여권, 신분증, 사업자등록번호 등 문서 정보를 저장합니다.
 * 검색 빈도가 낮으므로 별도 테이블로 유지합니다.
 */
@Entity
@Table(name = "entity_documents", indexes = {
        @Index(name = "idx_ed_entity_id", columnList = "entity_id"),
        @Index(name = "idx_ed_doc_type", columnList = "document_type"),
        @Index(name = "idx_ed_doc_number", columnList = "document_number"),
        @Index(name = "idx_ed_issuing_country", columnList = "issuing_country")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityDocumentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;
    
    /**
     * 부모 엔티티 참조
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    @ToString.Exclude
    private SanctionsEntity sanctionsEntity;
    
    /**
     * 문서 유형
     * - 'Passport': 여권
     * - 'National ID': 주민등록증/신분증
     * - 'Driver License': 운전면허증
     * - 'Tax ID': 납세자번호
     * - 'Business Registration': 사업자등록번호
     * - 'SSN': 사회보장번호
     * - 'Other': 기타
     */
    @Column(name = "document_type", length = 50)
    private String documentType;
    
    /**
     * 문서 번호
     */
    @Column(name = "document_number", length = 100)
    private String documentNumber;
    
    /**
     * 발급 국가
     */
    @Column(name = "issuing_country", length = 100)
    private String issuingCountry;
    
    /**
     * 발급 국가 ISO 코드
     */
    @Column(name = "issuing_country_code", length = 10)
    private String issuingCountryCode;
    
    /**
     * 발급일
     */
    @Column(name = "issue_date")
    private LocalDate issueDate;
    
    /**
     * 만료일
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    /**
     * 발급 기관
     */
    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority;
    
    /**
     * 비고
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
