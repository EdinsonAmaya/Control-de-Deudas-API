package com.edinsonamaya.control_deudas.debt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtRequest(
        @NotBlank(message = "El motivo/descripción es obligatorio")
        @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
        String description,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Positive(message = "El monto debe ser positivo")
        BigDecimal amount,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate date) {
}
