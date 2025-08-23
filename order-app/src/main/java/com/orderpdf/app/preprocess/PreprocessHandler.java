package com.orderpdf.app.preprocess;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderpdf.app.common.dto.OrderItem;
import com.orderpdf.app.common.dto.PreprocessOutput;
import com.orderpdf.app.preprocess.service.DynamoDBMessageParsingService;
import com.orderpdf.app.preprocess.service.OrderProcessingEligibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PreprocessHandler implements RequestHandler<SQSEvent, PreprocessOutput> {
    private static final Logger logger = LoggerFactory.getLogger(PreprocessHandler.class);
    
    private final DynamoDBMessageParsingService messageParsingService;
    private final OrderProcessingEligibilityService eligibilityService;

    public PreprocessHandler() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        String reprocessPolicy = System.getenv().getOrDefault("REPROCESS_POLICY", "FIRST_TIME_ONLY");
        
        this.messageParsingService = new DynamoDBMessageParsingService(objectMapper);
        this.eligibilityService = new OrderProcessingEligibilityService(reprocessPolicy);
    }

    // Constructor for testing with dependency injection
    public PreprocessHandler(DynamoDBMessageParsingService messageParsingService, 
                           OrderProcessingEligibilityService eligibilityService) {
        this.messageParsingService = messageParsingService;
        this.eligibilityService = eligibilityService;
    }

    @Override
    public PreprocessOutput handleRequest(SQSEvent sqsEvent, Context lambdaContext) {
        logger.info("Processing {} SQS messages", sqsEvent.getRecords().size());
        
        List<OrderItem> eligibleOrderItems = new ArrayList<>();
        
        for (SQSEvent.SQSMessage sqsMessage : sqsEvent.getRecords()) {
            try {
                OrderItem orderItem = messageParsingService.parseOrderItemFromMessage(sqsMessage.getBody());
                if (orderItem != null && eligibilityService.shouldProcessOrder(orderItem)) {
                    eligibleOrderItems.add(orderItem);
                    logger.info("Added eligible order item for processing: orderId={}", orderItem.orderId());
                }
            } catch (Exception exception) {
                logger.error("Failed to process SQS message: {}", sqsMessage.getMessageId(), exception);
            }
        }
        
        logger.info("Preprocessed {} eligible items out of {} total messages", 
            eligibleOrderItems.size(), sqsEvent.getRecords().size());
        
        return new PreprocessOutput(eligibleOrderItems, Instant.now());
    }
}