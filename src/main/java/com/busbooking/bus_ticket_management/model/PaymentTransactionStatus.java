package com.busbooking.bus_ticket_management.model;

// Kết quả
public enum PaymentTransactionStatus {
    PENDING,    // Giao dịch đang chờ xử lý
    SUCCESS,    // Giao dịch thành công
    FAILED      // Giao dịch thất bại
}