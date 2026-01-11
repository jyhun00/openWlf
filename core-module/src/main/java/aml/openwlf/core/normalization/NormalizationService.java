package aml.openwlf.core.normalization;

import aml.openwlf.core.model.CustomerInfo;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Service for normalizing customer data for better matching
 */
@Service
public class NormalizationService {
    
    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();
    
    /**
     * Normalize customer name for matching
     * - Convert to uppercase
     * - Remove extra spaces
     * - Remove special characters
     * - Sort name parts alphabetically
     */
    public String normalizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        
        // Remove accents and diacritics
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        
        // Convert to uppercase and remove special characters
        normalized = normalized.toUpperCase()
                              .replaceAll("[^A-Z0-9\\s]", "")
                              .replaceAll("\\s+", " ")
                              .trim();
        
        // Sort name parts alphabetically for better matching
        String[] parts = normalized.split("\\s+");
        Arrays.sort(parts);
        
        return Arrays.stream(parts)
                    .filter(part -> !part.isEmpty())
                    .collect(Collectors.joining(" "));
    }
    
    /**
     * Normalize nationality code
     */
    public String normalizeNationality(String nationality) {
        if (nationality == null || nationality.isEmpty()) {
            return "";
        }
        return nationality.toUpperCase().trim();
    }
    
    /**
     * Calculate similarity score between two strings (0.0 to 1.0)
     */
    public double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        String s1 = normalizeName(str1);
        String s2 = normalizeName(str2);
        
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        int distance = LEVENSHTEIN.apply(s1, s2);
        
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Check if name contains all words from another name (fuzzy match)
     */
    public boolean containsAllWords(String fullName, String searchName) {
        String[] searchWords = normalizeName(searchName).split("\\s+");
        String normalizedFull = normalizeName(fullName);
        
        for (String word : searchWords) {
            if (!normalizedFull.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
