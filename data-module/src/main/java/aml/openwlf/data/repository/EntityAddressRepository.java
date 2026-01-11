package aml.openwlf.data.repository;

import aml.openwlf.data.entity.EntityAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 주소 정보 Repository
 */
@Repository
public interface EntityAddressRepository extends JpaRepository<EntityAddressEntity, Long> {
    
    List<EntityAddressEntity> findBySanctionsEntityEntityId(Long entityId);
    
    List<EntityAddressEntity> findByCountry(String country);
    
    List<EntityAddressEntity> findByCountryCode(String countryCode);
    
    List<EntityAddressEntity> findByCity(String city);
    
    @Query("SELECT a FROM EntityAddressEntity a WHERE " +
           "LOWER(a.fullAddress) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.country) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<EntityAddressEntity> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT a.country, COUNT(a) FROM EntityAddressEntity a WHERE a.country IS NOT NULL GROUP BY a.country ORDER BY COUNT(a) DESC")
    List<Object[]> countByCountryGrouped();
}
