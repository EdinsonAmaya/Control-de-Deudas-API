package com.edinsonamaya.control_deudas.debt;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Debt d where d.id = :id")
    Optional<Debt> findByIdForUpdate(@Param("id") Long id);

    List<Debt> findByFolderIdOrderByCreatedAtDesc(Long folderId);

    @Query("""
            select new com.edinsonamaya.control_deudas.debt.DebtSumRow(
                p.debt.id, coalesce(sum(p.amount), 0))
            from Payment p
            where p.debt.folder.id = :folderId
            group by p.debt.id
            """)
    List<DebtSumRow> sumPaymentsByFolderId(@Param("folderId") Long folderId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.debt.id = :debtId")
    BigDecimal sumPaymentsByDebtId(@Param("debtId") Long debtId);
}
