package aml.openwlf.core.normalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizationServiceTest {
    
    private NormalizationService service;
    
    @BeforeEach
    void setUp() {
        service = new NormalizationService();
    }
    
    @Test
    void testNormalizeName_BasicNormalization() {
        String result = service.normalizeName("John Smith");
        assertEquals("JOHN SMITH", result);
    }
    
    @Test
    void testNormalizeName_RemoveSpecialCharacters() {
        String result = service.normalizeName("O'Neill-Brown");
        // 특수문자 제거 후 정렬됨: BROWN, ONEILLBROWN -> BROWN ONEILLBROWN 또는 단순히 특수문자만 제거
        // 실제 동작: O'Neill-Brown -> ONEILLBROWN (하이픈, 따옴표 제거) -> 단어 하나
        assertEquals("ONEILLBROWN", result);
    }

    @Test
    void testNormalizeName_RemoveAccents() {
        String result = service.normalizeName("José García");
        assertEquals("GARCIA JOSE", result);
    }

    @Test
    void testNormalizeName_RemoveExtraSpaces() {
        String result = service.normalizeName("John   Smith");
        assertEquals("JOHN SMITH", result);
    }

    @Test
    void testNormalizeName_SortParts() {
        String result = service.normalizeName("Smith John");
        assertEquals("JOHN SMITH", result);
    }

    @Test
    void testCalculateSimilarity_Exact() {
        double similarity = service.calculateSimilarity("John Smith", "John Smith");
        assertEquals(1.0, similarity, 0.01);
    }

    @Test
    void testCalculateSimilarity_Similar() {
        double similarity = service.calculateSimilarity("John Smith", "Jon Smith");
        // 정규화 후: JOHN SMITH vs JON SMITH -> 유사하지만 정확히 같지 않음
        assertTrue(similarity > 0.8, "Similarity should be > 0.8 but was " + similarity);
    }
    
    @Test
    void testCalculateSimilarity_Different() {
        double similarity = service.calculateSimilarity("John Smith", "Maria Garcia");
        assertTrue(similarity < 0.5);
    }
    
    @Test
    void testContainsAllWords_True() {
        boolean result = service.containsAllWords("John Michael Smith", "John Smith");
        assertTrue(result);
    }
    
    @Test
    void testContainsAllWords_False() {
        boolean result = service.containsAllWords("John Smith", "John Garcia");
        assertFalse(result);
    }
}
