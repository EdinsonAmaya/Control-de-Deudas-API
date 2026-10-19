package com.edinsonamaya.control_deudas.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debts/{debtId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> register(
            @PathVariable Long debtId,
            @RequestBody @Valid PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.register(debtId, request));
    }

    @GetMapping
    public ResponseEntity<PaymentHistoryResponse> history(@PathVariable Long debtId) {
        return ResponseEntity.ok(paymentService.history(debtId));
    }
}
