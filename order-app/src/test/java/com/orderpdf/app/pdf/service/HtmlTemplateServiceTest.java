package com.orderpdf.app.pdf.service;

import com.orderpdf.app.common.dto.Customer;
import com.orderpdf.app.common.dto.Order;
import com.orderpdf.app.common.dto.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class HtmlTemplateServiceTest {

    private HtmlTemplateService htmlTemplateService;

    @BeforeEach
    void setUp() {
        htmlTemplateService = new HtmlTemplateService();
    }

    private Order createOrder(String orderId, LocalDateTime createdAt, String status, String priority,
                             Customer customer, List<OrderLine> lines, String source, String region, 
                             String notes, Integer processingTime) {
        return new Order(
                orderId,
                "USD",
                createdAt.toInstant(ZoneOffset.UTC),
                customer,
                lines,
                status,
                notes,
                source,
                priority,
                region,
                null, // totalAmount
                createdAt.toLocalDate().toString(),
                processingTime
        );
    }

    @Nested
    @DisplayName("generateInvoiceHtml")
    class GenerateInvoiceHtmlTests {

        @Test
        @DisplayName("should generate HTML with basic order data")
        void shouldGenerateHtmlWithBasicOrderData() throws IOException {
            // Given
            Customer customer = new Customer("John Doe", "john@example.com", null, null);
            OrderLine orderLine = new OrderLine("ITEM-001", 2, 2500); // $25.00 each
            Order order = createOrder("ORD-123", LocalDateTime.of(2023, 12, 15, 10, 30), 
                    "confirmed", "high", customer, List.of(orderLine), "web", "US-EAST", 
                    "Priority order", 150);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).contains("ORD-123");
            assertThat(result).contains("Dec 15, 2023");
            assertThat(result).contains("CONFIRMED");
            assertThat(result).contains("HIGH");
            assertThat(result).contains("John Doe");
            assertThat(result).contains("john@example.com");
            assertThat(result).contains("ITEM-001");
            assertThat(result).contains("$50.00"); // subtotal (2 * $25.00)
            assertThat(result).contains("$4.00"); // tax (8% of $50.00)
            assertThat(result).contains("$54.00"); // grand total
        }

        @Test
        @DisplayName("should handle customer with phone number")
        void shouldHandleCustomerWithPhoneNumber() throws IOException {
            // Given
            Customer customer = new Customer("Jane Smith", "jane@example.com", "+1-555-123-4567", null);
            OrderLine orderLine = new OrderLine("ITEM-002", 1, 1000); // $10.00
            Order order = createOrder("ORD-124", LocalDateTime.of(2023, 12, 16, 14, 45),
                    "pending", "normal", customer, List.of(orderLine), null, null, null, null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("Jane Smith");
            assertThat(result).contains("jane@example.com");
            assertThat(result).contains("<div class=\"customer-detail\">Tel: +1-555-123-4567</div>");
            assertThat(result).contains("PENDING");
            assertThat(result).contains("NORMAL");
        }

        @Test
        @DisplayName("should handle customer without phone number")
        void shouldHandleCustomerWithoutPhoneNumber() throws IOException {
            // Given
            Customer customer = new Customer("Bob Johnson", "bob@example.com", null, null);
            OrderLine orderLine = new OrderLine("ITEM-003", 1, 1500);
            Order order = createOrder("ORD-125", LocalDateTime.of(2023, 12, 17, 9, 15),
                    "shipped", "low", customer, List.of(orderLine), "mobile", "EU-WEST", 
                    "Regular order", 75);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("Bob Johnson");
            assertThat(result).contains("bob@example.com");
            assertThat(result).doesNotContain("Tel:");
        }

        @Test
        @DisplayName("should handle customer with shipping address")
        void shouldHandleCustomerWithShippingAddress() throws IOException {
            // Given
            Customer customer = new Customer(
                    "Alice Brown", 
                    "alice@example.com", 
                    "+1-555-987-6543", 
                    "123 Main St, Apt 4B, New York, NY 10001"
            );
            OrderLine orderLine = new OrderLine("ITEM-004", 3, 800); // $8.00 each
            Order order = createOrder("ORD-126", LocalDateTime.of(2023, 12, 18, 16, 20),
                    "delivered", "urgent", customer, List.of(orderLine), "api", "US-WEST",
                    "Express delivery", 200);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("Alice Brown");
            assertThat(result).contains("<div class=\"customer-detail\">123 Main St</div>");
            assertThat(result).contains("<div class=\"customer-detail\">Apt 4B</div>");
            assertThat(result).contains("<div class=\"customer-detail\">New York</div>");
            assertThat(result).contains("<div class=\"customer-detail\">NY 10001</div>");
        }

        @Test
        @DisplayName("should handle customer without shipping address")
        void shouldHandleCustomerWithoutShippingAddress() throws IOException {
            // Given
            Customer customer = new Customer("Charlie Wilson", "charlie@example.com", null, null);
            OrderLine orderLine = new OrderLine("ITEM-005", 1, 2000);
            Order order = createOrder("ORD-127", LocalDateTime.of(2023, 12, 19, 11, 10),
                    "processing", null, customer, List.of(orderLine), null, null, null, null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("Charlie Wilson");
            assertThat(result).contains("<div class=\"customer-detail\" style=\"font-style: italic; color: #999;\">Same as billing address</div>");
        }

        @Test
        @DisplayName("should handle multiple order lines with correct calculations")
        void shouldHandleMultipleOrderLinesWithCorrectCalculations() throws IOException {
            // Given
            Customer customer = new Customer("David Lee", "david@example.com", null, null);
            List<OrderLine> orderLines = List.of(
                    new OrderLine("ITEM-001", 2, 1500), // $15.00 each = $30.00
                    new OrderLine("ITEM-002", 3, 1000), // $10.00 each = $30.00
                    new OrderLine("ITEM-003", 1, 4000)  // $40.00 each = $40.00
            );
            Order order = createOrder("ORD-128", LocalDateTime.of(2023, 12, 20, 13, 45),
                    "completed", "normal", customer, orderLines, "store", "US-CENTRAL",
                    "Bulk order", 300);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            // Check individual items
            assertThat(result).contains("ITEM-001");
            assertThat(result).contains("ITEM-002");
            assertThat(result).contains("ITEM-003");
            
            // Check quantities
            assertThat(result).contains("<td>2</td>");
            assertThat(result).contains("<td>3</td>");
            assertThat(result).contains("<td>1</td>");
            
            // Check unit prices
            assertThat(result).contains("$15.00");
            assertThat(result).contains("$10.00");
            assertThat(result).contains("$40.00");
            
            // Check line totals
            assertThat(result).contains("$30.00");
            assertThat(result).contains("$30.00");
            assertThat(result).contains("$40.00");
            
            // Check totals: subtotal = $100.00, tax = $8.00, total = $108.00
            assertThat(result).contains("$100.00"); // subtotal
            assertThat(result).contains("$8.00");   // tax
            assertThat(result).contains("$108.00"); // grand total
        }

        @Test
        @DisplayName("should handle empty order lines")
        void shouldHandleEmptyOrderLines() throws IOException {
            // Given
            Customer customer = new Customer("Emma Davis", "emma@example.com", null, null);
            Order order = createOrder("ORD-129", LocalDateTime.of(2023, 12, 21, 8, 30),
                    "cancelled", "low", customer, List.of(), "phone", "EU-NORTH",
                    "Cancelled order", 50);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("Emma Davis");
            assertThat(result).contains("CANCELLED");
            assertThat(result).contains("$0.00"); // subtotal should be $0.00
            assertThat(result).contains("$0.00"); // tax should be $0.00
            assertThat(result).contains("$0.00"); // grand total should be $0.00
        }

        @Test
        @DisplayName("should handle order with all metadata")
        void shouldHandleOrderWithAllMetadata() throws IOException {
            // Given
            Customer customer = new Customer("Frank Miller", "frank@example.com", "+1-555-456-7890", "456 Oak Ave, Suite 12, Chicago, IL 60601");
            OrderLine orderLine = new OrderLine("ITEM-PREMIUM", 1, 9999); // $99.99
            Order order = createOrder("ORD-130", LocalDateTime.of(2023, 12, 22, 15, 20),
                    "shipped", "urgent", customer, List.of(orderLine), "enterprise", "US-MIDWEST",
                    "VIP customer - expedite processing", 500);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("Frank Miller");
            assertThat(result).contains("SHIPPED");
            assertThat(result).contains("URGENT");
            assertThat(result).contains("Source: ENTERPRISE");
            assertThat(result).contains("Region: US-MIDWEST");
            assertThat(result).contains("Notes: VIP customer - expedite processing");
            assertThat(result).contains("Processing Time: 500");
        }

        @Test
        @DisplayName("should handle order with minimal metadata")
        void shouldHandleOrderWithMinimalMetadata() throws IOException {
            // Given
            Customer customer = new Customer("Grace Taylor", "grace@example.com", null, null);
            OrderLine orderLine = new OrderLine("ITEM-BASIC", 1, 999); // $9.99
            Order order = createOrder("ORD-131", LocalDateTime.of(2023, 12, 23, 12, 0),
                    null, null, customer, List.of(orderLine), null, null, null, null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("Grace Taylor");
            assertThat(result).contains("CONFIRMED"); // default status
            assertThat(result).contains("NORMAL");    // default priority
            assertThat(result).contains("Processing Time: 0"); // default processing time
            // Should not contain empty source, region, or notes sections
            assertThat(result).doesNotContain("Source:");
            assertThat(result).doesNotContain("Region:");
            assertThat(result).doesNotContain("Notes:");
        }

        @Test
        @DisplayName("should include generation date in output")
        void shouldIncludeGenerationDateInOutput() throws IOException {
            // Given
            Customer customer = new Customer("Henry Wilson", "henry@example.com", null, null);
            OrderLine orderLine = new OrderLine("ITEM-006", 1, 1200);
            Order order = createOrder("ORD-132", LocalDateTime.of(2023, 12, 24, 10, 15),
                    "confirmed", "normal", customer, List.of(orderLine), null, null, null, null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            // Should contain a generation date in the format yyyy-MM-dd HH:mm:ss
            assertThat(result).containsPattern("Generated on \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        }

        @Test
        @DisplayName("should handle decimal currency calculations correctly")
        void shouldHandleDecimalCurrencyCalculationsCorrectly() throws IOException {
            // Given
            Customer customer = new Customer("Ivy Chen", "ivy@example.com", null, null);
            List<OrderLine> orderLines = List.of(
                    new OrderLine("ITEM-A", 3, 333), // $3.33 each = $9.99 total
                    new OrderLine("ITEM-B", 2, 167)  // $1.67 each = $3.34 total
            );
            Order order = createOrder("ORD-133", LocalDateTime.of(2023, 12, 25, 9, 45),
                    "confirmed", "normal", customer, orderLines, null, null, null, null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("$3.33"); // unit price
            assertThat(result).contains("$1.67"); // unit price
            assertThat(result).contains("$9.99"); // line total
            assertThat(result).contains("$3.34"); // line total
            assertThat(result).contains("$13.33"); // subtotal ($9.99 + $3.34)
            assertThat(result).contains("$1.07");  // tax (8% of $13.33, rounded)
            assertThat(result).contains("$14.40"); // grand total ($13.33 + $1.07)
        }
    }

    @Nested
    @DisplayName("Template Loading")
    class TemplateLoadingTests {

        @Test
        @DisplayName("should successfully load and process template")
        void shouldSuccessfullyLoadAndProcessTemplate() throws IOException {
            // Given
            Customer customer = new Customer("Test User", "test@example.com", null, null);
            OrderLine orderLine = new OrderLine("TEST-ITEM", 1, 1000);
            Order order = createOrder("TEST-ORDER", LocalDateTime.of(2023, 12, 1, 12, 0),
                    "confirmed", "normal", customer, List.of(orderLine), null, null, null, null);

            // When & Then - Should not throw exception
            String result = htmlTemplateService.generateInvoiceHtml(order);
            
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result).contains("<!DOCTYPE html>");
            assertThat(result).contains("</html>");
            assertThat(result).doesNotContain("{{"); // All placeholders should be replaced
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle very long customer names")
        void shouldHandleVeryLongCustomerNames() throws IOException {
            // Given
            String longName = "A".repeat(100);
            Customer customer = new Customer(longName, "test@example.com", null, null);
            OrderLine orderLine = new OrderLine("ITEM-001", 1, 1000);
            Order order = createOrder("ORD-LONG", LocalDateTime.of(2023, 12, 1, 12, 0),
                    "confirmed", "normal", customer, List.of(orderLine), null, null, null, null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains(longName);
        }

        @Test
        @DisplayName("should handle special characters in customer data")
        void shouldHandleSpecialCharactersInCustomerData() throws IOException {
            // Given
            Customer customer = new Customer("João & María", "test+special@example.com", "+1-(555)-123-4567", null);
            OrderLine orderLine = new OrderLine("ITEM-SPECIAL-001", 1, 1000);
            Order order = createOrder("ORD-SPECIAL-123", LocalDateTime.of(2023, 12, 1, 12, 0),
                    "confirmed", "normal", customer, List.of(orderLine), null, null,
                    "Special notes with symbols: @#$%^&*()", null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("João & María");
            assertThat(result).contains("test+special@example.com");
            assertThat(result).contains("+1-(555)-123-4567");
            assertThat(result).contains("Special notes with symbols: @#$%^&*()");
        }

        @Test
        @DisplayName("should handle zero price items")
        void shouldHandleZeroPriceItems() throws IOException {
            // Given
            Customer customer = new Customer("Free User", "free@example.com", null, null);
            OrderLine orderLine = new OrderLine("FREE-ITEM", 5, 0); // Free item
            Order order = createOrder("ORD-FREE", LocalDateTime.of(2023, 12, 1, 12, 0),
                    "confirmed", "normal", customer, List.of(orderLine), null, null, null, null);

            // When
            String result = htmlTemplateService.generateInvoiceHtml(order);

            // Then
            assertThat(result).contains("FREE-ITEM");
            assertThat(result).contains("<td>5</td>"); // quantity
            assertThat(result).contains("$0.00"); // unit price, subtotal, tax, and grand total should all be $0.00
        }
    }
}