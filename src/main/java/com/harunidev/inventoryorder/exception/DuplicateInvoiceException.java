package com.harunidev.inventoryorder.exception;

public class DuplicateInvoiceException extends RuntimeException {
    public DuplicateInvoiceException(String invoiceNumber) {
        super(String.format("Invoice number '%s' already exists", invoiceNumber));
    }
}
