package com.orderpdf.app.common.service;

import com.orderpdf.app.common.dto.Order;
import com.orderpdf.app.common.dto.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDetailsServiceTest {

        @Mock
        private DynamoDbClient dynamoDbClient;

        private OrderDetailsService orderDetailsService;

        private static final String TEST_TABLE_NAME = "test-orders-table";

        @BeforeEach
        void setUp() {
                orderDetailsService = new OrderDetailsService(dynamoDbClient, TEST_TABLE_NAME);
        }

        @Test
        void shouldFetchOrderDetailsFromDynamoDB() {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#123",
                                "STATE#v1",
                                "123",
                                null);

                Map<String, AttributeValue> dynamoDbItem = createSampleDynamoDbItem();
                GetItemResponse response = GetItemResponse.builder()
                                .item(dynamoDbItem)
                                .build();

                when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

                // When
                Order result = orderDetailsService.fetchOrderDetails(orderItem);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.orderId()).isEqualTo("123");
                assertThat(result.customer().name()).isEqualTo("John Doe");
                assertThat(result.customer().email()).isEqualTo("john.doe@example.com");
                assertThat(result.customer().phone()).isEqualTo("+1-555-123-4567");
                assertThat(result.customer().address()).isEqualTo("123 Main St, Springfield, IL 62701");
                assertThat(result.status()).isEqualTo("CONFIRMED");
                assertThat(result.notes()).isEqualTo("Test order notes");
                assertThat(result.source()).isEqualTo("website");
                assertThat(result.priority()).isEqualTo("normal");
                assertThat(result.region()).isEqualTo("us-east");
                assertThat(result.totalAmount()).isEqualTo(125.50);
                assertThat(result.processingTime()).isEqualTo(1500);
                assertThat(result.lines()).hasSize(2);
                assertThat(result.lines().get(0).sku()).isEqualTo("ITEM-001");
                assertThat(result.lines().get(0).quantity()).isEqualTo(2);
                assertThat(result.lines().get(0).priceMinor()).isEqualTo(2500);
        }

        @Test
        void shouldReturnSampleOrderWhenDynamoDBItemNotFound() {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#999",
                                "STATE#v1",
                                "999",
                                null);

                GetItemResponse response = GetItemResponse.builder().build(); // No item

                when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

                // When
                Order result = orderDetailsService.fetchOrderDetails(orderItem);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.orderId()).isEqualTo("999");
                assertThat(result.customer().name()).isEqualTo("Sample Customer");
                assertThat(result.customer().email()).isEqualTo("customer@example.com");
                assertThat(result.customer().phone()).isEqualTo("+1-555-123-4567");
                assertThat(result.customer().address()).isEqualTo("123 Main Street, Springfield, CA 90210");
                assertThat(result.status()).isEqualTo("CONFIRMED");
                assertThat(result.notes()).isEqualTo("Sample order for testing");
                assertThat(result.lines()).hasSize(2);
        }

        @Test
        void shouldReturnSampleOrderWhenDynamoDBThrowsException() {
                // Given
                OrderItem orderItem = new OrderItem(
                                "ORDER#ERROR",
                                "STATE#v1",
                                "ERROR",
                                null);

                when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                                .thenThrow(new RuntimeException("DynamoDB connection failed"));

                // When
                Order result = orderDetailsService.fetchOrderDetails(orderItem);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.orderId()).isEqualTo("ERROR");
                assertThat(result.customer().name()).isEqualTo("Sample Customer");
                assertThat(result.status()).isEqualTo("CONFIRMED");
        }

        @Test
        void shouldHandlePartialDataFromDynamoDB() {
                // Given - DynamoDB item with some missing fields
                OrderItem orderItem = new OrderItem(
                                "ORDER#PARTIAL",
                                "STATE#v1",
                                "PARTIAL",
                                null);

                Map<String, AttributeValue> partialItem = Map.of(
                                "orderId", AttributeValue.builder().s("PARTIAL").build(),
                                "customerName", AttributeValue.builder().s("Jane Doe").build(),
                                "customerEmail", AttributeValue.builder().s("jane@example.com").build(),
                                "createdAt", AttributeValue.builder().s(Instant.now().toString()).build(),
                                "items", AttributeValue.builder().l(
                                                AttributeValue.builder().m(Map.of(
                                                                "itemId",
                                                                AttributeValue.builder().s("ITEM-001").build(),
                                                                "quantity", AttributeValue.builder().n("1").build(),
                                                                "price", AttributeValue.builder().n("25.00").build()))
                                                                .build())
                                                .build());

                GetItemResponse response = GetItemResponse.builder()
                                .item(partialItem)
                                .build();

                when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(response);

                // When
                Order result = orderDetailsService.fetchOrderDetails(orderItem);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.orderId()).isEqualTo("PARTIAL");
                assertThat(result.customer().name()).isEqualTo("Jane Doe");
                assertThat(result.customer().email()).isEqualTo("jane@example.com");
                assertThat(result.customer().phone()).isEqualTo(""); // Default for missing field
                assertThat(result.customer().address()).isEqualTo(""); // Default for missing field
                assertThat(result.status()).isEqualTo(""); // Default for missing field
                assertThat(result.lines()).hasSize(1);
                assertThat(result.lines().get(0).sku()).isEqualTo("ITEM-001");
                assertThat(result.lines().get(0).quantity()).isEqualTo(1);
                assertThat(result.lines().get(0).priceMinor()).isEqualTo(2500); // 25.00 * 100
        }

        private Map<String, AttributeValue> createSampleDynamoDbItem() {
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("orderId", AttributeValue.builder().s("123").build());
                item.put("customerName", AttributeValue.builder().s("John Doe").build());
                item.put("customerEmail", AttributeValue.builder().s("john.doe@example.com").build());
                item.put("customerPhone", AttributeValue.builder().s("+1-555-123-4567").build());
                item.put("shippingAddress", AttributeValue.builder().s("123 Main St, Springfield, IL 62701").build());
                item.put("status", AttributeValue.builder().s("CONFIRMED").build());
                item.put("notes", AttributeValue.builder().s("Test order notes").build());
                item.put("source", AttributeValue.builder().s("website").build());
                item.put("priority", AttributeValue.builder().s("normal").build());
                item.put("region", AttributeValue.builder().s("us-east").build());
                item.put("totalAmount", AttributeValue.builder().n("125.50").build());
                item.put("processingTime", AttributeValue.builder().n("1500").build());
                item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());

                // Create items list
                item.put("items", AttributeValue.builder().l(
                                AttributeValue.builder().m(Map.of(
                                                "itemId", AttributeValue.builder().s("ITEM-001").build(),
                                                "quantity", AttributeValue.builder().n("2").build(),
                                                "price", AttributeValue.builder().n("25.00").build())).build(),
                                AttributeValue.builder().m(Map.of(
                                                "itemId", AttributeValue.builder().s("ITEM-002").build(),
                                                "quantity", AttributeValue.builder().n("1").build(),
                                                "price", AttributeValue.builder().n("75.50").build())).build())
                                .build());

                return item;
        }
}
