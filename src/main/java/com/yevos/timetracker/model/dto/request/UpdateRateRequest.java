package com.yevos.timetracker.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRateRequest {

    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Hourly rate must be greater than 0")
    @Digits(integer = 6, fraction = 2,
            message = "Hourly rate format must be up to 6 digits and 2 decimals")
    private BigDecimal hourlyRate;

}
