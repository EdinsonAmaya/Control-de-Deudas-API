package com.edinsonamaya.control_deudas.folder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FolderDetailResponse(
        Long id,
        String name,
        BigDecimal totalDebt,
        LocalDateTime createdAt,
        java.util.List<com.edinsonamaya.control_deudas.debt.DebtResponse> debts) {
}
