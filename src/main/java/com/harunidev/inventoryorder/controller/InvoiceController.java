package com.harunidev.inventoryorder.controller;

import com.harunidev.inventoryorder.dto.request.InvoiceRequest;
import com.harunidev.inventoryorder.dto.response.ApiResponse;
import com.harunidev.inventoryorder.dto.response.InvoiceResponse;
import com.harunidev.inventoryorder.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management with duplicate prevention")
@SecurityRequirement(name = "Bearer Authentication")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create invoice for an order (ADMIN only, no duplicate numbers allowed)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully",
                        invoiceService.createInvoice(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all invoices (ADMIN only)")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAllInvoices() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAllInvoices()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get invoice by ID (ADMIN only)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoiceById(id)));
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get invoice by number (ADMIN only)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(
            @PathVariable String invoiceNumber) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoiceByNumber(invoiceNumber)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get invoice for a specific order")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByOrderId(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoiceByOrderId(orderId)));
    }

    @GetMapping("/unpaid")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all unpaid invoices (ADMIN only)")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getUnpaidInvoices() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getUnpaidInvoices()));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark invoice as paid (ADMIN only)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markAsPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice marked as paid",
                invoiceService.markAsPaid(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete invoice (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Invoice deleted successfully").build());
    }
}
