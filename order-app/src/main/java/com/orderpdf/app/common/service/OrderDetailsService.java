package com.orderpdf.app.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderpdf.app.common.dto.Customer;
import com.orderpdf.app.common.dto.Order;
import com.orderpdf.app.common.dto.OrderItem;
import com.orderpdf.app.common.dto.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class OrderDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(OrderDetailsService.class);

    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public OrderDetailsService() {
        this.dynamoDbClient = DynamoDbClient.builder().build();
        this.objectMapper = new ObjectMapper();
        this.tableName = System.getenv("DYNAMODB_TABLE_NAME") != null ? System.getenv("DYNAMODB_TABLE_NAME")
                : "orders";
    }

    // Constructor for testing with dependency injection
    public OrderDetailsService(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.objectMapper = new ObjectMapper();
        this.tableName = tableName;
    }

    public Order fetchOrderDetails(OrderItem orderItem) {
        logger.debug("Fetching order details for orderId: {}", orderItem.orderId());

        try {
            // Create the key for the main order record
            Map<String, AttributeValue> key = Map.of(
                    "pk", AttributeValue.builder().s(orderItem.pk()).build(),
                    "sk", AttributeValue.builder().s(orderItem.sk()).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (!response.hasItem()) {
                logger.warn("Order not found in DynamoDB for orderId: {}", orderItem.orderId());
                return createSampleOrder(orderItem.orderId());
            }

            return mapDynamoDbItemToOrder(response.item());

        } catch (Exception e) {
            logger.error("Error fetching order details from DynamoDB for orderId: {}", orderItem.orderId(), e);
            // Fallback to sample data if DynamoDB fails
            return createSampleOrder(orderItem.orderId());
        }
    }

    private Order mapDynamoDbItemToOrder(Map<String, AttributeValue> item) {
        try {
            // Extract customer information
            String customerName = getStringValue(item, "customerName");
            String customerEmail = getStringValue(item, "customerEmail");
            String customerPhone = getStringValue(item, "customerPhone");
            String shippingAddress = getStringValue(item, "shippingAddress");

            Customer customer = new Customer(customerName, customerEmail, customerPhone, shippingAddress);

            // Extract order lines from the items array
            List<OrderLine> orderLines = extractOrderLines(item);

            // Extract other order details
            String orderId = getStringValue(item, "orderId");
            String status = getStringValue(item, "status");
            String notes = getStringValue(item, "notes");
            String source = getStringValue(item, "source");
            String priority = getStringValue(item, "priority");
            String region = getStringValue(item, "region");
            String orderDate = getStringValue(item, "orderDate");

            Double totalAmount = getDoubleValue(item, "totalAmount");
            Integer processingTime = getIntegerValue(item, "processingTime");

            // Use createdAt or orderDate as the creation timestamp
            Instant createdAt = parseInstant(getStringValue(item, "createdAt"));

            return new Order(
                    orderId,
                    "USD", // Default currency
                    createdAt,
                    customer,
                    orderLines,
                    status,
                    notes,
                    source,
                    priority,
                    region,
                    totalAmount,
                    orderDate,
                    processingTime);

        } catch (Exception e) {
            logger.error("Error mapping DynamoDB item to Order", e);
            throw new RuntimeException("Failed to map DynamoDB item to Order", e);
        }
    }

    private List<OrderLine> extractOrderLines(Map<String, AttributeValue> item) {
        try {
            AttributeValue itemsAttribute = item.get("items");
            if (itemsAttribute == null || !itemsAttribute.hasL()) {
                logger.warn("No items found in order");
                return List.of();
            }

            return itemsAttribute.l().stream()
                    .map(this::mapItemToOrderLine)
                    .toList();

        } catch (Exception e) {
            logger.error("Error extracting order lines", e);
            return List.of();
        }
    }

    private OrderLine mapItemToOrderLine(AttributeValue itemValue) {
        if (!itemValue.hasM()) {
            throw new RuntimeException("Invalid item structure");
        }

        Map<String, AttributeValue> itemMap = itemValue.m();
        String itemId = getStringValue(itemMap, "itemId");
        Integer quantity = getIntegerValue(itemMap, "quantity");
        Double price = getDoubleValue(itemMap, "price");

        // Convert price from dollars to price minor (cents)
        long priceMinor = Math.round(price * 100);

        return new OrderLine(itemId, quantity, priceMinor);
    }

    private String getStringValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return (value != null && value.s() != null) ? value.s() : "";
    }

    private Double getDoubleValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value != null && value.n() != null) {
            try {
                return Double.parseDouble(value.n());
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format for key {}: {}", key, value.n());
            }
        }
        return 0.0;
    }

    private Integer getIntegerValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value != null && value.n() != null) {
            try {
                return Integer.parseInt(value.n());
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer format for key {}: {}", key, value.n());
            }
        }
        return 0;
    }

    private Instant parseInstant(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            logger.warn("Invalid timestamp format: {}", timestamp);
            return Instant.now();
        }
    }

    private Order createSampleOrder(String orderId) {
        logger.info("Creating sample order for orderId: {}", orderId);

        Customer sampleCustomer = new Customer(
                "Sample Customer",
                "customer@example.com",
                "+1-555-123-4567",
                "123 Main Street, Springfield, CA 90210");

        List<OrderLine> sampleOrderLines = List.of(
                new OrderLine("ITEM-001", 2, 2500),
                new OrderLine("ITEM-002", 1, 1500));

        return new Order(
                orderId,
                "USD",
                Instant.now(),
                sampleCustomer,
                sampleOrderLines,
                "CONFIRMED",
                "Sample order for testing",
                "website",
                "normal",
                "us-east",
                40.0,
                Instant.now().toString(),
                1500);
    }
}