package com.harunidev.inventoryorder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long orderId;
    private String orderNumber;
    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private Boolean isPaid;
    private String notes;
    private LocalDateTime createdAt;
}
