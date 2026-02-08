package aml.openwlf.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for member registration filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Member information for watchlist filtering during registration")
public class MemberFilterRequest {

    @NotBlank(message = "Member name is required")
    @Schema(description = "Member full name", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of birth", example = "1990-01-15")
    private LocalDate dateOfBirth;

    @Schema(description = "Nationality code (ISO 3166-1 alpha-2)", example = "KR")
    private String nationality;

    @Schema(description = "Address", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "Phone number", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "Email address", example = "hong@example.com")
    private String email;
}
