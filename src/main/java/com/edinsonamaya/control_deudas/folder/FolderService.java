package com.edinsonamaya.control_deudas.folder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edinsonamaya.control_deudas.common.error.ResourceNotFoundException;
import com.edinsonamaya.control_deudas.debt.Debt;
import com.edinsonamaya.control_deudas.debt.DebtRepository;
import com.edinsonamaya.control_deudas.debt.DebtResponse;
import com.edinsonamaya.control_deudas.debt.DebtStatus;
import com.edinsonamaya.control_deudas.debt.DebtSumRow;
import com.edinsonamaya.control_deudas.security.CurrentUserService;
import com.edinsonamaya.control_deudas.user.User;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public FolderResponse create(FolderRequest request) {
        User user = currentUserService.requireUser();
        Folder folder = Folder.builder()
                .name(request.name())
                .user(user)
                .build();
        Folder saved = folderRepository.save(folder);
        return new FolderResponse(saved.getId(), saved.getName(), BigDecimal.ZERO, saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listForCurrentUser() {
        User user = currentUserService.requireUser();
        List<Folder> folders = folderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (folders.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> debtSums = folderRepository.sumDebtAmountsByUserId(user.getId()).stream()
                .collect(Collectors.toMap(FolderSumRow::folderId, FolderSumRow::total, BigDecimal::add));
        Map<Long, BigDecimal> paymentSums = folderRepository.sumPaymentAmountsByUserId(user.getId()).stream()
                .collect(Collectors.toMap(FolderSumRow::folderId, FolderSumRow::total, BigDecimal::add));

        return folders.stream()
                .map(folder -> {
                    BigDecimal totalDebt = debtSums.getOrDefault(folder.getId(), BigDecimal.ZERO);
                    BigDecimal totalPaid = paymentSums.getOrDefault(folder.getId(), BigDecimal.ZERO);
                    BigDecimal balance = totalDebt.subtract(totalPaid);
                    if (balance.signum() < 0) {
                        balance = BigDecimal.ZERO;
                    }
                    return new FolderResponse(folder.getId(), folder.getName(), balance, folder.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public FolderDetailResponse getDetail(Long folderId) {
        User user = currentUserService.requireUser();
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Carpeta", folderId));
        if (folder.getUser() == null || !folder.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permiso para acceder a este recurso.");
        }

        List<Debt> debts = debtRepository.findByFolderIdOrderByCreatedAtDesc(folderId);
        Map<Long, BigDecimal> paidByDebt = debtRepository.sumPaymentsByFolderId(folderId).stream()
                .collect(Collectors.toMap(DebtSumRow::debtId, DebtSumRow::paid, BigDecimal::add));

        List<DebtResponse> debtResponses = debts.stream()
                .map(debt -> {
                    BigDecimal paid = paidByDebt.getOrDefault(debt.getId(), BigDecimal.ZERO);
                    BigDecimal balance = debt.getAmount().subtract(paid);
                    if (balance.signum() < 0) {
                        balance = BigDecimal.ZERO;
                    }
                    DebtStatus effectiveStatus = computeStatus(debt, paid, balance);
                    return new DebtResponse(
                            debt.getId(),
                            debt.getDescription(),
                            debt.getAmount(),
                            debt.getDueDate(),
                            debt.getCreatedAt(),
                            effectiveStatus,
                            paid,
                            balance);
                })
                .toList();

        BigDecimal folderTotal = debtResponses.stream()
                .map(DebtResponse::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FolderDetailResponse(
                folder.getId(),
                folder.getName(),
                folderTotal,
                folder.getCreatedAt(),
                debtResponses);
    }

    private DebtStatus computeStatus(Debt debt, BigDecimal paid, BigDecimal balance) {
        if (balance.signum() <= 0) {
            return DebtStatus.PAID;
        }
        if (paid.signum() > 0) {
            return DebtStatus.PARTIAL;
        }
        return debt.getStatus() == null ? DebtStatus.PENDING : debt.getStatus();
    }
}
