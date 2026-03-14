package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.dto.request.InvoiceRequest;
import com.harunidev.inventoryorder.dto.response.InvoiceResponse;
import com.harunidev.inventoryorder.entity.*;
import com.harunidev.inventoryorder.exception.DuplicateInvoiceException;
import com.harunidev.inventoryorder.exception.ResourceNotFoundException;
import com.harunidev.inventoryorder.repository.InvoiceRepository;
import com.harunidev.inventoryorder.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Order order;
    private Invoice invoice;
    private InvoiceRequest invoiceRequest;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .username("user1")
                .role(Role.USER)
                .build();

        order = Order.builder()
                .id(1L)
                .orderNumber("ORD-TESTABCD")
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("250.00"))
                .items(new ArrayList<>())
                .build();

        invoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-001")
                .order(order)
                .issuedAt(LocalDateTime.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(new BigDecimal("250.00"))
                .isPaid(false)
                .build();

        invoiceRequest = new InvoiceRequest();
        invoiceRequest.setInvoiceNumber("INV-001");
        invoiceRequest.setOrderId(1L);
        invoiceRequest.setDueDate(LocalDate.now().plusDays(30));
    }

    // ─── createInvoice ────────────────────────────────────────────────────────

    @Test
    void createInvoice_success() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = invoiceService.createInvoice(invoiceRequest);

        assertThat(response.getInvoiceNumber()).isEqualTo("INV-001");
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.getIsPaid()).isFalse();
    }

    @Test
    void createInvoice_duplicateInvoiceNumber_throwsException() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createInvoice(invoiceRequest))
                .isInstanceOf(DuplicateInvoiceException.class);

        verify(orderRepository, never()).findById(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoice_orderNotFound_throwsException() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(invoiceRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void createInvoice_orderAlreadyHasInvoice_throwsException() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.createInvoice(invoiceRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an invoice");

        verify(invoiceRepository, never()).save(any());
    }

    // ─── getInvoiceById ───────────────────────────────────────────────────────

    @Test
    void getInvoiceById_found() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void getInvoiceById_notFound_throwsException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── getInvoiceByNumber ───────────────────────────────────────────────────

    @Test
    void getInvoiceByNumber_found() {
        when(invoiceRepository.findByInvoiceNumber("INV-001")).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceByNumber("INV-001");

        assertThat(response.getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void getInvoiceByNumber_notFound_throwsException() {
        when(invoiceRepository.findByInvoiceNumber("INV-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceByNumber("INV-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getInvoiceByOrderId ──────────────────────────────────────────────────

    @Test
    void getInvoiceByOrderId_found() {
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceByOrderId(1L);

        assertThat(response.getOrderId()).isEqualTo(1L);
    }

    @Test
    void getInvoiceByOrderId_notFound_throwsException() {
        when(invoiceRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceByOrderId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── markAsPaid ───────────────────────────────────────────────────────────

    @Test
    void markAsPaid_success() {
        Invoice paidInvoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-001")
                .order(order)
                .issuedAt(LocalDateTime.now())
                .totalAmount(new BigDecimal("250.00"))
                .isPaid(true)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(paidInvoice);

        InvoiceResponse response = invoiceService.markAsPaid(1L);

        assertThat(response.getIsPaid()).isTrue();
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markAsPaid_invoiceNotFound_throwsException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.markAsPaid(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getUnpaidInvoices ────────────────────────────────────────────────────

    @Test
    void getUnpaidInvoices_returnsOnlyUnpaid() {
        when(invoiceRepository.findByIsPaid(false)).thenReturn(List.of(invoice));

        List<InvoiceResponse> unpaid = invoiceService.getUnpaidInvoices();

        assertThat(unpaid).hasSize(1);
        assertThat(unpaid.get(0).getIsPaid()).isFalse();
    }

    // ─── getAllInvoices ───────────────────────────────────────────────────────

    @Test
    void getAllInvoices_returnsList() {
        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        List<InvoiceResponse> all = invoiceService.getAllInvoices();

        assertThat(all).hasSize(1);
    }

    // ─── deleteInvoice ────────────────────────────────────────────────────────

    @Test
    void deleteInvoice_success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        invoiceService.deleteInvoice(1L);

        verify(invoiceRepository).deleteById(1L);
    }

    @Test
    void deleteInvoice_notFound_throwsException() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.deleteInvoice(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(invoiceRepository, never()).deleteById(any());
    }
}
