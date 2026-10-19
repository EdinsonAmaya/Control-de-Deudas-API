package com.edinsonamaya.control_deudas.payment;

import com.edinsonamaya.control_deudas.debt.DebtStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        LocalDate paymentDate,
        String notes,
        LocalDateTime createdAt) {
}
