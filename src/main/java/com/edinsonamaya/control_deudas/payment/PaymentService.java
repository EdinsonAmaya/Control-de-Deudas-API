package com.edinsonamaya.control_deudas.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edinsonamaya.control_deudas.common.error.BusinessRuleException;
import com.edinsonamaya.control_deudas.common.error.ResourceNotFoundException;
import com.edinsonamaya.control_deudas.debt.Debt;
import com.edinsonamaya.control_deudas.debt.DebtRepository;
import com.edinsonamaya.control_deudas.debt.DebtStatus;
import com.edinsonamaya.control_deudas.security.CurrentUserService;
import com.edinsonamaya.control_deudas.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final DebtRepository debtRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    /**
     * Registra un abono sobre una deuda. Bloquea la fila de la deuda
     * (SELECT ... FOR UPDATE) para evitar condiciones de carrera cuando
     * se registran abonos simultaneos, y valida que el abono no supere
     * el saldo pendiente. Toda la operacion es atomica.
     */
    @Transactional
    public PaymentResponse register(Long debtId, PaymentRequest request) {
        User user = currentUserService.requireUser();
        Debt debt = debtRepository.findByIdForUpdate(debtId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda", debtId));
        requireOwnership(debt, user);

        if (debt.getStatus() == DebtStatus.PAID) {
            throw new BusinessRuleException("La deuda ya está completamente pagada.");
        }

        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El monto del abono debe ser mayor a cero.");
        }

        BigDecimal paid = debtRepository.sumPaymentsByDebtId(debtId);
        BigDecimal balance = debt.getAmount().subtract(paid);

        if (amount.compareTo(balance) > 0) {
            throw new BusinessRuleException(
                    "El monto del abono no puede ser mayor al saldo pendiente de la deuda.");
        }

        LocalDate paymentDate = request.date() != null ? request.date() : LocalDate.now();
        Payment payment = Payment.builder()
                .debt(debt)
                .amount(amount)
                .paymentDate(paymentDate)
                .notes(request.notes())
                .build();
        Payment saved = paymentRepository.save(payment);

        BigDecimal newPaid = paid.add(amount);
        debt.setStatus(newPaid.compareTo(debt.getAmount()) >= 0 ? DebtStatus.PAID : DebtStatus.PARTIAL);
        debtRepository.save(debt);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentHistoryResponse history(Long debtId) {
        User user = currentUserService.requireUser();
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new ResourceNotFoundException("Deuda", debtId));
        requireOwnership(debt, user);

        BigDecimal paid = debtRepository.sumPaymentsByDebtId(debtId);
        BigDecimal balance = debt.getAmount().subtract(paid);
        if (balance.signum() < 0) {
            balance = BigDecimal.ZERO;
        }

        List<PaymentResponse> payments = paymentRepository.findByDebtIdOrderByCreatedAtDesc(debtId)
                .stream()
                .map(this::toResponse)
                .toList();

        return new PaymentHistoryResponse(
                debt.getId(),
                debt.getAmount(),
                paid,
                balance,
                debt.getStatus(),
                payments);
    }

    private void requireOwnership(Debt debt, User user) {
        if (debt.getFolder() == null
                || debt.getFolder().getUser() == null
                || !debt.getFolder().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("No tiene permiso para acceder a este recurso.");
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        LocalDateTime createdAt = payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now();
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getNotes(),
                createdAt);
    }
}
