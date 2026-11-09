package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTransactionTest {

    private PaymentTransaction createTransaction() {
        return new PaymentTransaction(
                new Ticket(),
                new BigDecimal("150000.00"),
                PaymentMethodType.BANK_TRANSFER
        );
    }

    @Test
    void newTransactionShouldBePending() {
        PaymentTransaction transaction = createTransaction();

        assertEquals(
                PaymentTransactionStatus.PENDING,
                transaction.getStatus()
        );
        assertNull(transaction.getCompletedAt());
    }

    @Test
    void successfulPaymentShouldRecordReferenceAndCompletionTime() {
        PaymentTransaction transaction = createTransaction();

        transaction.markSuccess("  DEMO-001  ");

        assertEquals(
                PaymentTransactionStatus.SUCCESS,
                transaction.getStatus()
        );
        assertEquals("DEMO-001", transaction.getProviderReference());
        assertNotNull(transaction.getCompletedAt());
        assertNull(transaction.getFailureReason());
    }

    @Test
    void failedPaymentShouldRecordReason() {
        PaymentTransaction transaction = createTransaction();

        transaction.markFailed("  Số dư không đủ  ");

        assertEquals(
                PaymentTransactionStatus.FAILED,
                transaction.getStatus()
        );
        assertEquals("Số dư không đủ", transaction.getFailureReason());
        assertNotNull(transaction.getCompletedAt());
    }

    @Test
    void completedTransactionShouldRejectAnotherResult() {
        PaymentTransaction successful = createTransaction();
        successful.markSuccess("DEMO-002");

        assertThrows(
                IllegalStateException.class,
                () -> successful.markFailed("Lỗi đến muộn")
        );
        assertEquals(
                PaymentTransactionStatus.SUCCESS,
                successful.getStatus()
        );

        PaymentTransaction failed = createTransaction();
        failed.markFailed("Giao dịch bị từ chối");

        assertThrows(
                IllegalStateException.class,
                () -> failed.markSuccess("DEMO-003")
        );
        assertEquals(
                PaymentTransactionStatus.FAILED,
                failed.getStatus()
        );
    }

    @Test
    void invalidAmountShouldBeRejected() {
        for (BigDecimal amount : new BigDecimal[]{
                null, BigDecimal.ZERO, new BigDecimal("-1000")
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PaymentTransaction(
                            new Ticket(),
                            amount,
                            PaymentMethodType.CASH
                    )
            );
        }
    }

    @Test
    void invalidFailureReasonShouldLeaveTransactionPending() {
        PaymentTransaction transaction = createTransaction();

        for (String reason : new String[]{
                null, "   ", "x".repeat(256)
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> transaction.markFailed(reason)
            );
        }

        assertEquals(
                PaymentTransactionStatus.PENDING,
                transaction.getStatus()
        );
        assertNull(transaction.getCompletedAt());
        assertNull(transaction.getFailureReason());
    }

    @Test
    void oversizedReferenceShouldLeaveTransactionPending() {
        PaymentTransaction transaction = createTransaction();

        assertThrows(
                IllegalArgumentException.class,
                () -> transaction.markSuccess("x".repeat(101))
        );

        assertEquals(
                PaymentTransactionStatus.PENDING,
                transaction.getStatus()
        );
        assertNull(transaction.getProviderReference());
        assertNull(transaction.getCompletedAt());
    }
}