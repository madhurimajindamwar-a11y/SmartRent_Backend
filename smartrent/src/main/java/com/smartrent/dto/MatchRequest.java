package com.smartrent.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MatchRequest(

        @DecimalMin(
                value = "0.01",
                message = "Maximum rent must be greater than zero"
        )
        @Digits(integer = 8, fraction = 2)
        BigDecimal maxRent,

        @Size(max = 100)
        String locality,

        @Size(max = 30)
        String roomType,

        Boolean wifi,
        Boolean food,
        Boolean ac

) {
}