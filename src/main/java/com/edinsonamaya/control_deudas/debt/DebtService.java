package com.edinsonamaya.control_deudas.debt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edinsonamaya.control_deudas.common.error.BusinessRuleException;
import com.edinsonamaya.control_deudas.common.error.ResourceNotFoundException;
import com.edinsonamaya.control_deudas.folder.Folder;
import com.edinsonamaya.control_deudas.folder.FolderRepository;
import com.edinsonamaya.control_deudas.security.CurrentUserService;
import com.edinsonamaya.control_deudas.user.User;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final FolderRepository folderRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public DebtResponse create(Long folderId, DebtRequest request) {
        User user = currentUserService.requireUser();
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Carpeta", folderId));
        if (folder.getUser() == null || !folder.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("No tiene permiso para acceder a este recurso.");
        }

        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El monto de la deuda debe ser mayor a cero.");
        }

        Debt debt = Debt.builder()
                .folder(folder)
                .description(request.description())
                .amount(amount)
                .dueDate(request.date())
                .status(DebtStatus.PENDING)
                .build();
        Debt saved = debtRepository.save(debt);

        return new DebtResponse(
                saved.getId(),
                saved.getDescription(),
                saved.getAmount(),
                saved.getDueDate(),
                saved.getCreatedAt(),
                saved.getStatus(),
                BigDecimal.ZERO,
                saved.getAmount());
    }

    @Transactional(readOnly = true)
    public java.util.List<DebtResponse> listForFolder(Long folderId) {
        User user = currentUserService.requireUser();
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Carpeta", folderId));
        if (folder.getUser() == null || !folder.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("No tiene permiso para acceder a este recurso.");
        }

        java.util.Map<Long, BigDecimal> paidByDebt = debtRepository.sumPaymentsByFolderId(folderId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        DebtSumRow::debtId, DebtSumRow::paid, BigDecimal::add));

        return debtRepository.findByFolderIdOrderByCreatedAtDesc(folderId).stream()
                .map(debt -> {
                    BigDecimal paid = paidByDebt.getOrDefault(debt.getId(), BigDecimal.ZERO);
                    BigDecimal balance = debt.getAmount().subtract(paid);
                    if (balance.signum() < 0) {
                        balance = BigDecimal.ZERO;
                    }
                    DebtStatus status = balance.signum() <= 0
                            ? DebtStatus.PAID
                            : (paid.signum() > 0 ? DebtStatus.PARTIAL : DebtStatus.PENDING);
                    return new DebtResponse(
                            debt.getId(),
                            debt.getDescription(),
                            debt.getAmount(),
                            debt.getDueDate(),
                            debt.getCreatedAt(),
                            status,
                            paid,
                            balance);
                })
                .toList();
    }
}
