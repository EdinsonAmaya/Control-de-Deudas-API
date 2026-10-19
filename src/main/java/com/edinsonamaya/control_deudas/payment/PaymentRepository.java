package com.edinsonamaya.control_deudas.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByDebtIdOrderByCreatedAtDesc(Long debtId);
}
