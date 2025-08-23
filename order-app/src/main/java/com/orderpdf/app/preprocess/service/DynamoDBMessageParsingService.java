package com.orderpdf.app.preprocess.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderpdf.app.common.dto.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBMessageParsingService {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBMessageParsingService.class);

    private final ObjectMapper objectMapper;

    public DynamoDBMessageParsingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OrderItem parseOrderItemFromMessage(String messageBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(messageBody);

            JsonNode dynamoDBNode = rootNode.path("dynamodb");
            if (dynamoDBNode.isMissingNode()) {
                logger.warn("No DynamoDB data found in message");
                return null;
            }

            JsonNode newImageNode = dynamoDBNode.path("NewImage");
            if (newImageNode.isMissingNode()) {
                logger.warn("No NewImage found in DynamoDB record");
                return null;
            }

            String primaryKey = extractStringValue(newImageNode, "pk");
            String sortKey = extractStringValue(newImageNode, "sk");
            String orderId = extractStringValue(newImageNode, "orderId");
            String oldPdfKey = extractNestedStringValue(newImageNode, "pdf", "s3Key");

            if (primaryKey == null || sortKey == null || orderId == null) {
                logger.warn("Missing required fields: primaryKey={}, sortKey={}, orderId={}",
                        primaryKey, sortKey, orderId);
                return null;
            }

            return new OrderItem(primaryKey, sortKey, orderId, oldPdfKey);

        } catch (Exception exception) {
            logger.error("Failed to parse order item from message body", exception);
            return null;
        }
    }

    private String extractStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode()) {
            return null;
        }
        return fieldNode.path("S").asText(null);
    }

    private String extractNestedStringValue(JsonNode node, String parentField, String childField) {
        JsonNode parentNode = node.path(parentField);
        if (parentNode.isMissingNode()) {
            return null;
        }
        JsonNode mapValue = parentNode.path("M");
        if (mapValue.isMissingNode()) {
            return null;
        }
        return extractStringValue(mapValue, childField);
    }
}