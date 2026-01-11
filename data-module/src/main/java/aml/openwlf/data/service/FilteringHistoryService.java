package aml.openwlf.data.service;

import aml.openwlf.core.model.FilteringResult;
import aml.openwlf.data.entity.FilteringHistoryEntity;
import aml.openwlf.data.repository.FilteringHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing filtering history
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilteringHistoryService {
    
    private final FilteringHistoryRepository repository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public FilteringHistoryEntity saveFilteringResult(FilteringResult result) {
        try {
            FilteringHistoryEntity entity = FilteringHistoryEntity.builder()
                    .customerId(result.getCustomerInfo().getCustomerId())
                    .customerName(result.getCustomerInfo().getName())
                    .dateOfBirth(result.getCustomerInfo().getDateOfBirth())
                    .nationality(result.getCustomerInfo().getNationality())
                    .isAlert(result.isAlert())
                    .score(result.getScore())
                    .matchedRules(objectMapper.writeValueAsString(result.getMatchedRules()))
                    .explanation(result.getExplanation())
                    .build();
            
            FilteringHistoryEntity saved = repository.save(entity);
            log.info("Saved filtering history: id={}, alert={}", saved.getId(), saved.getIsAlert());
            return saved;
            
        } catch (Exception e) {
            log.error("Failed to save filtering history", e);
            throw new RuntimeException("Failed to save filtering history", e);
        }
    }
    
    public List<FilteringHistoryEntity> getAlertHistory() {
        return repository.findByIsAlertTrue();
    }
    
    public List<FilteringHistoryEntity> getHistoryByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId);
    }
}
