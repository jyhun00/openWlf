package aml.openwlf.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Customer information for watchlist filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInfo {
    private String name;
    private LocalDate dateOfBirth;
    private String nationality;
    private String customerId;
}
