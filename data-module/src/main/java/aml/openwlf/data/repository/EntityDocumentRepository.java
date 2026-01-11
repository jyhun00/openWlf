package aml.openwlf.data.repository;

import aml.openwlf.data.entity.EntityDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 문서/신분증 정보 Repository
 */
@Repository
public interface EntityDocumentRepository extends JpaRepository<EntityDocumentEntity, Long> {
    
    List<EntityDocumentEntity> findBySanctionsEntityEntityId(Long entityId);
    
    List<EntityDocumentEntity> findByDocumentType(String documentType);
    
    List<EntityDocumentEntity> findByDocumentNumber(String documentNumber);
    
    List<EntityDocumentEntity> findByIssuingCountry(String issuingCountry);
    
    List<EntityDocumentEntity> findByIssuingCountryCode(String issuingCountryCode);
    
    /**
     * 문서 번호로 검색 (부분 일치)
     */
    @Query("SELECT d FROM EntityDocumentEntity d WHERE d.documentNumber LIKE CONCAT('%', :number, '%')")
    List<EntityDocumentEntity> searchByDocumentNumber(@Param("number") String number);
    
    /**
     * 여권 번호로 엔티티 검색
     */
    @Query("SELECT d FROM EntityDocumentEntity d WHERE d.documentType = 'Passport' AND d.documentNumber = :passportNumber")
    List<EntityDocumentEntity> findByPassportNumber(@Param("passportNumber") String passportNumber);
    
    @Query("SELECT d.documentType, COUNT(d) FROM EntityDocumentEntity d GROUP BY d.documentType")
    List<Object[]> countByDocumentTypeGrouped();
    
    @Query("SELECT d.issuingCountry, COUNT(d) FROM EntityDocumentEntity d WHERE d.issuingCountry IS NOT NULL GROUP BY d.issuingCountry ORDER BY COUNT(d) DESC")
    List<Object[]> countByIssuingCountryGrouped();
}
