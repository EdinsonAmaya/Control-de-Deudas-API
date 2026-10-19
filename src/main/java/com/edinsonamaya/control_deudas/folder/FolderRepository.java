package com.edinsonamaya.control_deudas.folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select new com.edinsonamaya.control_deudas.folder.FolderSumRow(
                d.folder.id, coalesce(sum(d.amount), 0))
            from Debt d
            where d.folder.user.id = :userId
            group by d.folder.id
            """)
    List<FolderSumRow> sumDebtAmountsByUserId(@Param("userId") Long userId);

    @Query("""
            select new com.edinsonamaya.control_deudas.folder.FolderSumRow(
                p.debt.folder.id, coalesce(sum(p.amount), 0))
            from Payment p
            where p.debt.folder.user.id = :userId
            group by p.debt.folder.id
            """)
    List<FolderSumRow> sumPaymentAmountsByUserId(@Param("userId") Long userId);
}
