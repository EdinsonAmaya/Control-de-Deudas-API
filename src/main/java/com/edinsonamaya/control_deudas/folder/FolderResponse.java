package com.edinsonamaya.control_deudas.folder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FolderResponse(Long id, String name, BigDecimal totalDebt, LocalDateTime createdAt) {
}
