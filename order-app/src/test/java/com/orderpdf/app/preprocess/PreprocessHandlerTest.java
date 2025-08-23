package com.orderpdf.app.preprocess;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.orderpdf.app.common.dto.OrderItem;
import com.orderpdf.app.common.dto.PreprocessOutput;
import com.orderpdf.app.preprocess.service.DynamoDBMessageParsingService;
import com.orderpdf.app.preprocess.service.OrderProcessingEligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreprocessHandlerTest {

    @Mock
    private Context lambdaContext;

    @Mock
    private DynamoDBMessageParsingService messageParsingService;

    @Mock
    private OrderProcessingEligibilityService eligibilityService;

    private PreprocessHandler preprocessHandler;

    @BeforeEach
    void setUp() {
        preprocessHandler = new PreprocessHandler(messageParsingService, eligibilityService);
    }

    @Test
    void shouldProcessValidSQSMessage() {
        String messageBody = "sample-message-body";
        OrderItem orderItem = new OrderItem("ORDER#123", "STATE#v1", "123", null);

        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(messageBody);

        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(List.of(sqsMessage));

        when(messageParsingService.parseOrderItemFromMessage(messageBody)).thenReturn(orderItem);
        when(eligibilityService.shouldProcessOrder(orderItem)).thenReturn(true);

        PreprocessOutput result = preprocessHandler.handleRequest(sqsEvent, lambdaContext);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0)).isEqualTo(orderItem);
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void shouldSkipIneligibleOrders() {
        String messageBody = "sample-message-body";
        OrderItem orderItem = new OrderItem("ORDER#123", "STATE#v1", "123", "old-pdf-key");

        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(messageBody);

        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(List.of(sqsMessage));

        when(messageParsingService.parseOrderItemFromMessage(messageBody)).thenReturn(orderItem);
        when(eligibilityService.shouldProcessOrder(orderItem)).thenReturn(false);

        PreprocessOutput result = preprocessHandler.handleRequest(sqsEvent, lambdaContext);

        assertThat(result.items()).isEmpty();
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void shouldHandleNullOrderItems() {
        String messageBody = "invalid-message-body";

        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(messageBody);

        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(List.of(sqsMessage));

        when(messageParsingService.parseOrderItemFromMessage(messageBody)).thenReturn(null);

        PreprocessOutput result = preprocessHandler.handleRequest(sqsEvent, lambdaContext);

        assertThat(result.items()).isEmpty();
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void shouldHandleEmptyRecords() {
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(List.of());

        PreprocessOutput result = preprocessHandler.handleRequest(sqsEvent, lambdaContext);

        assertThat(result.items()).isEmpty();
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void shouldHandleExceptionsDuringProcessing() {
        String messageBody = "sample-message-body";

        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(messageBody);
        sqsMessage.setMessageId("test-message-id");

        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(List.of(sqsMessage));

        when(messageParsingService.parseOrderItemFromMessage(messageBody))
                .thenThrow(new RuntimeException("Parsing failed"));

        PreprocessOutput result = preprocessHandler.handleRequest(sqsEvent, lambdaContext);

        assertThat(result.items()).isEmpty();
        assertThat(result.timestamp()).isNotNull();
    }
}