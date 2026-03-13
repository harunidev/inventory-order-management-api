package com.harunidev.inventoryorder.repository;

import com.harunidev.inventoryorder.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByOrderId(Long orderId);
    boolean existsByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByIsPaid(Boolean isPaid);
}
