package com.edinsonamaya.control_deudas.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull(message = "El monto del abono es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto del abono debe ser mayor a cero")
        @Positive(message = "El monto del abono debe ser positivo")
        BigDecimal amount,

        LocalDate date,

        @Size(max = 255, message = "La nota debe tener máximo 255 caracteres")
        String notes) {
}
