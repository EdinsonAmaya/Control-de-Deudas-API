package com.edinsonamaya.control_deudas.payment;

import com.edinsonamaya.control_deudas.debt.DebtStatus;

import java.math.BigDecimal;
import java.util.List;

public record PaymentHistoryResponse(
        Long debtId,
        BigDecimal amount,
        BigDecimal paid,
        BigDecimal balance,
        DebtStatus status,
        List<PaymentResponse> payments) {
}
