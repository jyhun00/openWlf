package aml.openwlf.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 매칭 알고리즘 테스트 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매칭 알고리즘 테스트 요청")
public class MatchingTestRequest {
    
    @NotBlank(message = "첫 번째 이름을 입력해주세요")
    @Schema(description = "첫 번째 이름", example = "Muhammad Ali")
    private String name1;
    
    @NotBlank(message = "두 번째 이름을 입력해주세요")
    @Schema(description = "두 번째 이름", example = "Mohammed Ali")
    private String name2;
    
    @Schema(description = "테스트할 알고리즘 (ALL, SOUNDEX, METAPHONE, JARO_WINKLER, NGRAM, KOREAN, COMPOSITE)", 
            example = "ALL", defaultValue = "ALL")
    private String algorithm;
}
