package com.orderpdf.app.pdf.service;

import com.orderpdf.app.common.dto.Customer;
import com.orderpdf.app.common.dto.Order;
import com.orderpdf.app.common.dto.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentGenerationServiceTest {

    private PdfDocumentGenerationService pdfDocumentGenerationService;

    @BeforeEach
    void setUp() {
        pdfDocumentGenerationService = new PdfDocumentGenerationService();
    }

    @Test
    void shouldGeneratePdfWithBasicOrderData() throws Exception {
        // Given
        Order order = createBasicOrder();

        // When
        byte[] pdfBytes = pdfDocumentGenerationService.generatePdfDocument(order);

        // Then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(pdfBytes).startsWith("%PDF".getBytes()); // PDF magic number
    }

    @Test
    void shouldGeneratePdfWithRichOrderData() throws Exception {
        // Given
        Order order = createRichOrder();

        // When
        byte[] pdfBytes = pdfDocumentGenerationService.generatePdfDocument(order);

        // Then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(pdfBytes).startsWith("%PDF".getBytes()); // PDF magic number
    }

    @Test
    void shouldGeneratePdfWithMinimalOrderData() throws Exception {
        // Given
        Order order = createMinimalOrder();

        // When
        byte[] pdfBytes = pdfDocumentGenerationService.generatePdfDocument(order);

        // Then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(pdfBytes).startsWith("%PDF".getBytes()); // PDF magic number
    }

    @Test
    void shouldGeneratePdfWithMultipleItems() throws Exception {
        // Given
        Order order = createOrderWithMultipleItems();

        // When
        byte[] pdfBytes = pdfDocumentGenerationService.generatePdfDocument(order);

        // Then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(pdfBytes).startsWith("%PDF".getBytes()); // PDF magic number
    }

    @Test
    void shouldGeneratePdfWithLongAddress() throws Exception {
        // Given
        Order order = createOrderWithLongAddress();

        // When
        byte[] pdfBytes = pdfDocumentGenerationService.generatePdfDocument(order);

        // Then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(pdfBytes).startsWith("%PDF".getBytes()); // PDF magic number
    }

    @Test
    void shouldHandleEmptyOrderLines() throws Exception {
        // Given
        Order order = createOrderWithEmptyLines();

        // When
        byte[] pdfBytes = pdfDocumentGenerationService.generatePdfDocument(order);

        // Then
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
        assertThat(pdfBytes).startsWith("%PDF".getBytes()); // PDF magic number
    }

    @Test
    void shouldHandleNullOrder() {
        // When/Then
        assertThatThrownBy(() -> pdfDocumentGenerationService.generatePdfDocument(null))
                .isInstanceOf(Exception.class);
    }

    private Order createBasicOrder() {
        Customer customer = new Customer(
                "John Doe",
                "john.doe@example.com",
                "+1-555-123-4567",
                "123 Main Street, Springfield, IL 62701");

        List<OrderLine> orderLines = List.of(
                new OrderLine("ITEM-001", 2, 2500),
                new OrderLine("ITEM-002", 1, 1500));

        return new Order(
                "ORD-12345",
                "USD",
                Instant.now(),
                customer,
                orderLines,
                "CONFIRMED",
                "Standard order",
                "website",
                "normal",
                "us-east",
                40.0,
                Instant.now().toString(),
                1200);
    }

    private Order createRichOrder() {
        Customer customer = new Customer(
                "Jane Smith",
                "jane.smith@company.com",
                "+1-555-987-6543",
                "456 Corporate Drive, Suite 200, Business City, NY 10001");

        List<OrderLine> orderLines = List.of(
                new OrderLine("SKU-PREMIUM-001", 1, 9999),
                new OrderLine("SKU-DELUXE-002", 2, 4999),
                new OrderLine("SKU-STANDARD-003", 3, 1999));

        return new Order(
                "ORDER-2024-789",
                "USD",
                Instant.now(),
                customer,
                orderLines,
                "PROCESSING",
                "Rush order - please expedite shipping and handle with care",
                "mobile_app",
                "high",
                "us-west",
                259.95,
                Instant.now().toString(),
                2850);
    }

    private Order createMinimalOrder() {
        Customer customer = new Customer(
                "Minimal Customer",
                "min@example.com",
                "",
                "");

        List<OrderLine> orderLines = List.of(
                new OrderLine("MIN-001", 1, 1000));

        return new Order(
                "MIN-001",
                "USD",
                Instant.now(),
                customer,
                orderLines,
                "",
                "",
                "",
                "",
                "",
                10.0,
                Instant.now().toString(),
                500);
    }

    private Order createOrderWithMultipleItems() {
        Customer customer = new Customer(
                "Multi Customer",
                "multi@example.com",
                "+1-555-111-2222",
                "789 Multi Street, Multi City, MC 54321");

        List<OrderLine> orderLines = List.of(
                new OrderLine("ITEM-A", 1, 1000),
                new OrderLine("ITEM-B", 2, 1500),
                new OrderLine("ITEM-C", 3, 2000),
                new OrderLine("ITEM-D", 1, 5000),
                new OrderLine("ITEM-E", 4, 750),
                new OrderLine("ITEM-F", 2, 3250));

        return new Order(
                "MULTI-123",
                "USD",
                Instant.now(),
                customer,
                orderLines,
                "SHIPPED",
                "Large order with multiple items",
                "api",
                "normal",
                "eu-central",
                245.0,
                Instant.now().toString(),
                3200);
    }

    private Order createOrderWithLongAddress() {
        Customer customer = new Customer(
                "Long Address Customer",
                "long.address@verylongdomain.example.com",
                "+1-555-999-8888",
                "1234 Very Long Street Name With Multiple Words, Apartment Complex Building A Unit 567, " +
                        "Very Long City Name, Very Long State Name 98765-4321");

        List<OrderLine> orderLines = List.of(
                new OrderLine("LONG-ITEM-001", 1, 2500));

        return new Order(
                "LONG-ADDRESS-789",
                "USD",
                Instant.now(),
                customer,
                orderLines,
                "DELIVERED",
                "Order with very long address information for testing PDF layout",
                "phone",
                "urgent",
                "ap-southeast",
                25.0,
                Instant.now().toString(),
                1800);
    }

    private Order createOrderWithEmptyLines() {
        Customer customer = new Customer(
                "Empty Lines Customer",
                "empty@example.com",
                "+1-555-000-0000",
                "000 Empty Street, Empty City, EC 00000");

        return new Order(
                "EMPTY-001",
                "USD",
                Instant.now(),
                customer,
                List.of(), // Empty order lines
                "PENDING",
                "Order with no items",
                "store",
                "low",
                "us-central",
                0.0,
                Instant.now().toString(),
                100);
    }
}
