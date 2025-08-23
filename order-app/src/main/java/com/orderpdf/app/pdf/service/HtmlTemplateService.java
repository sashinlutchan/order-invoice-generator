package com.orderpdf.app.pdf.service;

import com.orderpdf.app.common.dto.Order;
import com.orderpdf.app.common.dto.OrderLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class HtmlTemplateService {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final String TEMPLATE_PATH = "/templates/invoice-template.html";

    public String generateInvoiceHtml(Order order) throws IOException {
        // Load template from resources
        String template = loadTemplate();

        // Calculate totals
        double subtotal = order.lines().stream()
                .mapToDouble(line -> (line.priceMinor() / 100.0) * line.quantity())
                .sum();
        double taxRate = 0.08;
        double taxAmount = subtotal * taxRate;
        double grandTotal = subtotal + taxAmount;

        String formattedDate = order.createdAt()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

        // Replace placeholders with actual data
        return processTemplate(template, order, formattedDate, subtotal, taxAmount, grandTotal);
    }

    private String loadTemplate() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (inputStream == null) {
                throw new IOException("Template file not found: " + TEMPLATE_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String processTemplate(String template, Order order, String formattedDate,
            double subtotal, double taxAmount, double grandTotal) {
        return template
                .replace("{{ORDER_ID}}", order.orderId())
                .replace("{{ORDER_DATE}}", formattedDate)
                .replace("{{ORDER_STATUS}}", order.status() != null ? order.status().toUpperCase() : "CONFIRMED")
                .replace("{{ORDER_PRIORITY}}", order.priority() != null ? order.priority().toUpperCase() : "NORMAL")
                .replace("{{CUSTOMER_NAME}}", order.customer().name())
                .replace("{{CUSTOMER_EMAIL}}", order.customer().email())
                .replace("{{CUSTOMER_PHONE}}", generatePhoneSection(order.customer().phone()))
                .replace("{{SHIPPING_ADDRESS}}", generateShippingAddress(order))
                .replace("{{ORDER_META}}", generateOrderMeta(order))
                .replace("{{ORDER_ITEMS}}", generateItemRows(order))
                .replace("{{SUBTOTAL}}", CURRENCY_FORMAT.format(subtotal))
                .replace("{{TAX_AMOUNT}}", CURRENCY_FORMAT.format(taxAmount))
                .replace("{{GRAND_TOTAL}}", CURRENCY_FORMAT.format(grandTotal))
                .replace("{{GENERATION_DATE}}",
                        java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .replace("{{PROCESSING_TIME}}",
                        String.valueOf(order.processingTime() != null ? order.processingTime() : 0));
    }

    private String generatePhoneSection(String phone) {
        return phone != null && !phone.isEmpty()
                ? "<div class=\"customer-detail\">Tel: " + phone + "</div>"
                : "";
    }

    private String generateShippingAddress(Order order) {
        if (order.customer().address() != null && !order.customer().address().isEmpty()) {
            String[] addressLines = order.customer().address().split(",");
            StringBuilder address = new StringBuilder();
            for (String line : addressLines) {
                address.append("<div class=\"customer-detail\">").append(line.trim()).append("</div>");
            }
            return address.toString();
        } else {
            return "<div class=\"customer-detail\" style=\"font-style: italic; color: #999;\">Same as billing address</div>";
        }
    }

    private String generateOrderMeta(Order order) {
        StringBuilder meta = new StringBuilder("<div class=\"order-meta\">");

        if (order.source() != null && !order.source().isEmpty()) {
            meta.append("<span>Source: ").append(order.source().toUpperCase()).append("</span>");
        }
        if (order.region() != null && !order.region().isEmpty()) {
            meta.append("<span>Region: ").append(order.region().toUpperCase()).append("</span>");
        }
        if (order.notes() != null && !order.notes().isEmpty()) {
            meta.append("<span>Notes: ").append(order.notes()).append("</span>");
        }

        meta.append("</div>");
        return meta.toString();
    }

    private String generateItemRows(Order order) {
        StringBuilder rows = new StringBuilder();

        for (OrderLine line : order.lines()) {
            double unitPrice = line.priceMinor() / 100.0;
            double totalPrice = unitPrice * line.quantity();

            rows.append("""
                    <tr>
                        <td><span class="item-sku">%s</span></td>
                        <td>%d</td>
                        <td>%s</td>
                        <td>%s</td>
                    </tr>
                    """.formatted(
                    line.sku(),
                    line.quantity(),
                    CURRENCY_FORMAT.format(unitPrice),
                    CURRENCY_FORMAT.format(totalPrice)));
        }

        return rows.toString();
    }
}