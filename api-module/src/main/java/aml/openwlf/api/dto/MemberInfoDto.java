package aml.openwlf.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for member information response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Member information")
public class MemberInfoDto {

    @Schema(description = "Member name", example = "홍길동")
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of birth", example = "1990-01-15")
    private LocalDate dateOfBirth;

    @Schema(description = "Nationality", example = "KR")
    private String nationality;

    @Schema(description = "Address", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "Phone number", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "Email address", example = "hong@example.com")
    private String email;
}
