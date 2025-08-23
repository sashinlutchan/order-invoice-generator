package com.orderpdf.app.pdf;

import com.amazonaws.services.lambda.runtime.Context;
import com.orderpdf.app.common.dto.Customer;
import com.orderpdf.app.common.dto.GeneratePdfOutput;
import com.orderpdf.app.common.dto.Order;
import com.orderpdf.app.common.dto.OrderItem;
import com.orderpdf.app.common.dto.OrderLine;
import com.orderpdf.app.common.service.OrderDetailsService;
import com.orderpdf.app.common.util.S3Helper;
import com.orderpdf.app.pdf.service.PdfDocumentGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratePdfHandlerTest {

        @Mock
        private Context lambdaContext;

        @Mock
        private PdfDocumentGenerationService pdfGenerationService;

        @Mock
        private OrderDetailsService orderDetailsService;

        @Mock
        private S3Helper s3StorageHelper;

        private GeneratePdfHandler generatePdfHandler;

        @BeforeEach
        void setUp() {
                generatePdfHandler = new GeneratePdfHandler(
                                pdfGenerationService,
                                orderDetailsService,
                                s3StorageHelper);
        }

        @Test
        void shouldGeneratePdfSuccessfully() throws Exception {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#123",
                                "STATE#v1",
                                "123",
                                null);

                byte[] pdfBytes = "fake-pdf-bytes".getBytes();
                Order orderDetails = createSampleOrder("123");

                when(lambdaContext.getAwsRequestId()).thenReturn("test-execution-id");
                when(orderDetailsService.fetchOrderDetails(orderItem))
                                .thenReturn(orderDetails);
                when(pdfGenerationService.generatePdfDocument(orderDetails))
                                .thenReturn(pdfBytes);

                // When
                GeneratePdfOutput result = generatePdfHandler.handleRequest(orderItem, lambdaContext);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.tempPdfKey()).isEqualTo("temp/test-execution-id-123.pdf");

                verify(s3StorageHelper).putObjectFromBytes(
                                eq("temp/test-execution-id-123.pdf"),
                                eq(pdfBytes),
                                eq("application/pdf"));
        }

        @Test
        void shouldHandleOrderDetailsServiceFailure() throws Exception {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#123",
                                "STATE#v1",
                                "123",
                                null);

                when(orderDetailsService.fetchOrderDetails(orderItem))
                                .thenThrow(new RuntimeException("DynamoDB connection failed"));

                // When/Then
                assertThatThrownBy(() -> generatePdfHandler.handleRequest(orderItem, lambdaContext))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("PDF invoice generation failed for order: 123");
        }

        @Test
        void shouldHandlePdfGenerationFailure() throws Exception {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#123",
                                "STATE#v1",
                                "123",
                                null);

                Order orderDetails = createSampleOrder("123");

                when(orderDetailsService.fetchOrderDetails(orderItem))
                                .thenReturn(orderDetails);
                when(pdfGenerationService.generatePdfDocument(orderDetails))
                                .thenThrow(new RuntimeException("PDF generation failed"));

                // When/Then
                assertThatThrownBy(() -> generatePdfHandler.handleRequest(orderItem, lambdaContext))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("PDF invoice generation failed for order: 123");
        }

        @Test
        void shouldHandleS3UploadFailure() throws Exception {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#123",
                                "STATE#v1",
                                "123",
                                null);

                byte[] pdfBytes = "fake-pdf-bytes".getBytes();
                Order orderDetails = createSampleOrder("123");

                when(lambdaContext.getAwsRequestId()).thenReturn("test-execution-id");
                when(orderDetailsService.fetchOrderDetails(orderItem))
                                .thenReturn(orderDetails);
                when(pdfGenerationService.generatePdfDocument(orderDetails))
                                .thenReturn(pdfBytes);
                doThrow(new RuntimeException("S3 upload failed"))
                                .when(s3StorageHelper).putObjectFromBytes(any(), any(), any());

                // When/Then
                assertThatThrownBy(() -> generatePdfHandler.handleRequest(orderItem, lambdaContext))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("PDF invoice generation failed for order: 123");
        }

        @Test
        void shouldCreateCorrectTemporaryPdfKey() throws Exception {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#456",
                                "STATE#v1",
                                "456",
                                null);

                byte[] pdfBytes = "fake-pdf-bytes".getBytes();
                Order orderDetails = createSampleOrder("456");

                when(lambdaContext.getAwsRequestId()).thenReturn("test-execution-id");
                when(orderDetailsService.fetchOrderDetails(orderItem))
                                .thenReturn(orderDetails);
                when(pdfGenerationService.generatePdfDocument(orderDetails))
                                .thenReturn(pdfBytes);

                // When
                GeneratePdfOutput result = generatePdfHandler.handleRequest(orderItem, lambdaContext);

                // Then
                assertThat(result.tempPdfKey()).isEqualTo("temp/test-execution-id-456.pdf");
        }

        @Test
        void shouldGeneratePdfWithRichCustomerData() throws Exception {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#789",
                                "STATE#v1",
                                "789",
                                null);

                byte[] pdfBytes = "fake-pdf-bytes".getBytes();
                Order orderDetails = createRichSampleOrder("789");

                when(lambdaContext.getAwsRequestId()).thenReturn("test-execution-id");
                when(orderDetailsService.fetchOrderDetails(orderItem))
                                .thenReturn(orderDetails);
                when(pdfGenerationService.generatePdfDocument(orderDetails))
                                .thenReturn(pdfBytes);

                // When
                GeneratePdfOutput result = generatePdfHandler.handleRequest(orderItem, lambdaContext);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.tempPdfKey()).isEqualTo("temp/test-execution-id-789.pdf");

                verify(orderDetailsService).fetchOrderDetails(orderItem);
                verify(pdfGenerationService).generatePdfDocument(orderDetails);
                verify(s3StorageHelper).putObjectFromBytes(
                                eq("temp/test-execution-id-789.pdf"),
                                eq(pdfBytes),
                                eq("application/pdf"));
        }

        private Order createSampleOrder(String orderId) {
                Customer customer = new Customer(
                                "Test Customer",
                                "test@example.com",
                                "+1-555-123-4567",
                                "123 Test Street, Test City, TC 12345");

                List<OrderLine> orderLines = List.of(
                                new OrderLine("ITEM-001", 2, 2500),
                                new OrderLine("ITEM-002", 1, 1500));

                return new Order(
                                orderId,
                                "USD",
                                Instant.now(),
                                customer,
                                orderLines,
                                "CONFIRMED",
                                "Test order",
                                "website",
                                "normal",
                                "us-east",
                                40.0,
                                Instant.now().toString(),
                                1500);
        }

        private Order createRichSampleOrder(String orderId) {
                Customer customer = new Customer(
                                "John Doe",
                                "john.doe@example.com",
                                "+1-555-987-6543",
                                "456 Main Avenue, Springfield, IL 62701");

                List<OrderLine> orderLines = List.of(
                                new OrderLine("SKU-001", 3, 4999),
                                new OrderLine("SKU-002", 1, 2499),
                                new OrderLine("SKU-003", 2, 1299));

                return new Order(
                                orderId,
                                "USD",
                                Instant.now(),
                                customer,
                                orderLines,
                                "PROCESSING",
                                "Priority order - please handle with care",
                                "mobile_app",
                                "high",
                                "us-west",
                                124.95,
                                Instant.now().toString(),
                                2250);
        }
}