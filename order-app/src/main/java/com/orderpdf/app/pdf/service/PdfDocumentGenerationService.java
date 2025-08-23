package com.orderpdf.app.pdf.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.orderpdf.app.common.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfDocumentGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(PdfDocumentGenerationService.class);
    private final HtmlTemplateService htmlTemplateService;

    public PdfDocumentGenerationService() {
        this.htmlTemplateService = new HtmlTemplateService();
    }

    public byte[] generatePdfDocument(Order order) throws IOException {
        try {
            // Generate HTML from template
            String html = htmlTemplateService.generateInvoiceHtml(order);

            // Convert HTML to PDF using iText
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html, outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            logger.error("Failed to generate PDF for order: {}", order.orderId(), e);
            throw new IOException("PDF generation failed", e);
        }
    }

}
