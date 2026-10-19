package com.edinsonamaya.control_deudas.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import com.edinsonamaya.control_deudas.common.error.BusinessRuleException;
import com.edinsonamaya.control_deudas.common.error.ResourceNotFoundException;
import com.edinsonamaya.control_deudas.debt.Debt;
import com.edinsonamaya.control_deudas.debt.DebtRepository;
import com.edinsonamaya.control_deudas.debt.DebtStatus;
import com.edinsonamaya.control_deudas.folder.Folder;
import com.edinsonamaya.control_deudas.security.CurrentUserService;
import com.edinsonamaya.control_deudas.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long DEBT_ID = 100L;
    private static final BigDecimal DEBT_AMOUNT = new BigDecimal("500.00");

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PaymentService paymentService;

    private User owner;
    private User intruder;
    private Debt debt;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("owner@test.com")
                .password("$2a$10$hash")
                .fullName("Propietario")
                .build();
        intruder = User.builder()
                .id(2L)
                .email("intruder@test.com")
                .password("$2a$10$hash")
                .fullName("Intruso")
                .build();
        Folder folder = Folder.builder()
                .id(10L)
                .name("Personal")
                .user(owner)
                .build();
        debt = Debt.builder()
                .id(DEBT_ID)
                .folder(folder)
                .description("Deuda de prueba")
                .amount(DEBT_AMOUNT)
                .status(DebtStatus.PENDING)
                .dueDate(LocalDate.now().plusMonths(1))
                .build();
    }

    @Test
    void registraAbonoParcialYMarcaDeudaComoParcial() {
        when(currentUserService.requireUser()).thenReturn(owner);
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));
        when(debtRepository.sumPaymentsByDebtId(DEBT_ID)).thenReturn(new BigDecimal("200.00"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment saved = inv.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        when(debtRepository.save(any(Debt.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.register(
                DEBT_ID, new PaymentRequest(new BigDecimal("100.00"), LocalDate.now(), "abono parcial"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.notes()).isEqualTo("abono parcial");
        assertThat(debt.getStatus()).isEqualTo(DebtStatus.PARTIAL);
        verify(paymentRepository).save(any(Payment.class));
        verify(debtRepository).save(debt);
    }

    @Test
    void registraAbonoTotalYMarcaDeudaComoPagada() {
        when(currentUserService.requireUser()).thenReturn(owner);
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));
        when(debtRepository.sumPaymentsByDebtId(DEBT_ID)).thenReturn(new BigDecimal("450.00"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(debtRepository.save(any(Debt.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.register(
                DEBT_ID, new PaymentRequest(new BigDecimal("50.00"), null, null));

        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(response.paymentDate()).isEqualTo(LocalDate.now());
        assertThat(debt.getStatus()).isEqualTo(DebtStatus.PAID);
    }

    @Test
    void rechazaAbonoMayorAlSaldoPendiente() {
        when(currentUserService.requireUser()).thenReturn(owner);
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));
        when(debtRepository.sumPaymentsByDebtId(DEBT_ID)).thenReturn(new BigDecimal("400.00"));

        assertThatThrownBy(() -> paymentService.register(
                DEBT_ID, new PaymentRequest(new BigDecimal("200.00"), null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no puede ser mayor al saldo pendiente");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void rechazaAbonoEnDeudaYaPagada() {
        debt.setStatus(DebtStatus.PAID);
        when(currentUserService.requireUser()).thenReturn(owner);
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));

        assertThatThrownBy(() -> paymentService.register(
                DEBT_ID, new PaymentRequest(new BigDecimal("10.00"), null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya está completamente pagada");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void rechazaAbonoConMontoNoPositivo() {
        when(currentUserService.requireUser()).thenReturn(owner);
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));

        assertThatThrownBy(() -> paymentService.register(
                DEBT_ID, new PaymentRequest(BigDecimal.ZERO, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void lanza404CuandoLaDeudaNoExiste() {
        when(currentUserService.requireUser()).thenReturn(owner);
        when(debtRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.register(
                999L, new PaymentRequest(BigDecimal.TEN, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lanza403CuandoLaDeudaPerteneceAOtroUsuario() {
        when(currentUserService.requireUser()).thenReturn(intruder);
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));

        assertThatThrownBy(() -> paymentService.register(
                DEBT_ID, new PaymentRequest(BigDecimal.TEN, null, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permiso");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void historialCalculaSaldoYListadoDeAbonos() {
        when(currentUserService.requireUser()).thenReturn(owner);
        when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(debt));
        when(debtRepository.sumPaymentsByDebtId(DEBT_ID)).thenReturn(new BigDecimal("300.00"));
        Payment first = Payment.builder()
                .id(1L)
                .debt(debt)
                .amount(new BigDecimal("200.00"))
                .paymentDate(LocalDate.now().minusDays(2))
                .notes("abono 1")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();
        Payment second = Payment.builder()
                .id(2L)
                .debt(debt)
                .amount(new BigDecimal("100.00"))
                .paymentDate(LocalDate.now())
                .notes("abono 2")
                .createdAt(LocalDateTime.now())
                .build();
        when(paymentRepository.findByDebtIdOrderByCreatedAtDesc(DEBT_ID))
                .thenReturn(List.of(second, first));

        PaymentHistoryResponse history = paymentService.history(DEBT_ID);

        assertThat(history.debtId()).isEqualTo(DEBT_ID);
        assertThat(history.amount()).isEqualByComparingTo("500.00");
        assertThat(history.paid()).isEqualByComparingTo("300.00");
        assertThat(history.balance()).isEqualByComparingTo("200.00");
        assertThat(history.payments()).hasSize(2);
        assertThat(history.payments().get(0).notes()).isEqualTo("abono 2");
    }

    @Test
    void historialLanza403ParaDeudaDeOtroUsuario() {
        when(currentUserService.requireUser()).thenReturn(intruder);
        when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(debt));

        assertThatThrownBy(() -> paymentService.history(DEBT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }
}
