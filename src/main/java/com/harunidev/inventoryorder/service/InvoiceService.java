package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.dto.request.InvoiceRequest;
import com.harunidev.inventoryorder.dto.response.InvoiceResponse;
import com.harunidev.inventoryorder.entity.Invoice;
import com.harunidev.inventoryorder.entity.Order;
import com.harunidev.inventoryorder.exception.DuplicateInvoiceException;
import com.harunidev.inventoryorder.exception.ResourceNotFoundException;
import com.harunidev.inventoryorder.repository.InvoiceRepository;
import com.harunidev.inventoryorder.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    /**
     * Creates an invoice for an order.
     * BUSINESS RULE: Duplicate invoice numbers are not allowed.
     * BUSINESS RULE: An order can only have one invoice.
     */
    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        // Business Rule: No duplicate invoice numbers
        if (invoiceRepository.existsByInvoiceNumber(request.getInvoiceNumber())) {
            throw new DuplicateInvoiceException(request.getInvoiceNumber());
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + request.getOrderId()));

        // Business Rule: Order cannot already have an invoice
        if (invoiceRepository.findByOrderId(order.getId()).isPresent()) {
            throw new IllegalStateException(
                    "Order '" + order.getOrderNumber() + "' already has an invoice");
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .order(order)
                .issuedAt(LocalDateTime.now())
                .dueDate(request.getDueDate())
                .totalAmount(order.getTotalAmount())
                .isPaid(false)
                .notes(request.getNotes())
                .build();

        return mapToResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse getInvoiceById(Long id) {
        return mapToResponse(findById(id));
    }

    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        return mapToResponse(invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with number: " + invoiceNumber)));
    }

    public InvoiceResponse getInvoiceByOrderId(Long orderId) {
        return mapToResponse(invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No invoice found for order id: " + orderId)));
    }

    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse markAsPaid(Long id) {
        Invoice invoice = findById(id);
        invoice.setIsPaid(true);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    public List<InvoiceResponse> getUnpaidInvoices() {
        return invoiceRepository.findByIsPaid(false).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteInvoice(Long id) {
        findById(id);
        invoiceRepository.deleteById(id);
    }

    private Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .orderId(invoice.getOrder().getId())
                .orderNumber(invoice.getOrder().getOrderNumber())
                .issuedAt(invoice.getIssuedAt())
                .dueDate(invoice.getDueDate())
                .totalAmount(invoice.getTotalAmount())
                .isPaid(invoice.getIsPaid())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
