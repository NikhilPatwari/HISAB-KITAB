package com.hisabkitab.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class EmployerDtos {

    private EmployerDtos() {
    }

    public record EmployerRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 160, message = "Name is too long")
            String name,

            @Size(max = 32, message = "Phone is too long")
            String phone,

            @Size(max = 1000, message = "Notes are too long")
            String notes,

            Boolean active) {
    }

    /**
     * @param netOutstanding money this employer has put out that is still unrecovered,
     *                       as a positive number
     */
    public record EmployerView(
            Long id,
            String name,
            String phone,
            String notes,
            boolean active,
            BigDecimal netOutstanding) {
    }
}
