package com.orderpdf.app.pdf;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.orderpdf.app.common.dto.GeneratePdfOutput;
import com.orderpdf.app.common.dto.Order;
import com.orderpdf.app.common.dto.OrderItem;
import com.orderpdf.app.common.service.OrderDetailsService;
import com.orderpdf.app.common.util.S3Helper;
import com.orderpdf.app.pdf.service.PdfDocumentGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class GeneratePdfHandler implements RequestHandler<OrderItem, GeneratePdfOutput> {
    private static final Logger logger = LoggerFactory.getLogger(GeneratePdfHandler.class);

    private final PdfDocumentGenerationService pdfGenerationService;
    private final OrderDetailsService orderDetailsService;
    private final S3Helper s3StorageHelper;

    public GeneratePdfHandler() {
        String bucketName = System.getenv("BUCKET_NAME");

        S3Client s3Client = S3Client.builder().build();

        this.pdfGenerationService = new PdfDocumentGenerationService();
        this.orderDetailsService = new OrderDetailsService();
        this.s3StorageHelper = new S3Helper(s3Client, bucketName);
    }

    // Constructor for testing with dependency injection
    public GeneratePdfHandler(PdfDocumentGenerationService pdfGenerationService,
            OrderDetailsService orderDetailsService,
            S3Helper s3StorageHelper) {
        this.pdfGenerationService = pdfGenerationService;
        this.orderDetailsService = orderDetailsService;
        this.s3StorageHelper = s3StorageHelper;
    }

    @Override
    public GeneratePdfOutput handleRequest(OrderItem orderItem, Context lambdaContext) {
        logger.info("Generating PDF invoice for orderId: {}", orderItem.orderId());

        try {
            Order orderDetails = orderDetailsService.fetchOrderDetails(orderItem);

            byte[] pdfDocumentBytes = pdfGenerationService.generatePdfDocument(orderDetails);

            String executionId = lambdaContext.getAwsRequestId();
            String temporaryPdfKey = createTemporaryPdfKey(executionId, orderItem.orderId());

            s3StorageHelper.putObjectFromBytes(temporaryPdfKey, pdfDocumentBytes, "application/pdf");

            logger.info("Successfully generated PDF invoice for orderId: {}, temporaryKey: {}",
                    orderItem.orderId(), temporaryPdfKey);

            return new GeneratePdfOutput(temporaryPdfKey);

        } catch (Exception exception) {
            logger.error("Failed to generate PDF invoice for orderId: {}", orderItem.orderId(), exception);
            throw new RuntimeException("PDF invoice generation failed for order: " + orderItem.orderId(), exception);
        }
    }

    private String createTemporaryPdfKey(String executionId, String orderId) {
        return String.format("temp/%s-%s.pdf", executionId, orderId);
    }
}