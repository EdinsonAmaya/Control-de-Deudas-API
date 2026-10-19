package com.edinsonamaya.control_deudas.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DebtResponse(
        Long id,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime createdAt,
        DebtStatus status,
        BigDecimal paid,
        BigDecimal balance) {
}
