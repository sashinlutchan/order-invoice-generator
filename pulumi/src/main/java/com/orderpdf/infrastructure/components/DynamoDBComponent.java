package com.orderpdf.infrastructure.components;

import com.pulumi.aws.dynamodb.Table;
import com.pulumi.aws.dynamodb.TableArgs;
import com.pulumi.aws.dynamodb.inputs.TableAttributeArgs;
import com.pulumi.core.Output;

public class DynamoDBComponent {
    
    public static class Builder {
        private String tableName;

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public DynamoDBComponent build() {
            return new DynamoDBComponent(tableName);
        }
    }

    private final Table ordersTable;

    private DynamoDBComponent(String tableName) {
        this.ordersTable = new Table(tableName, TableArgs.builder()
            .name(tableName)
            .billingMode("PAY_PER_REQUEST")
            .attributes(
                TableAttributeArgs.builder()
                    .name("pk")
                    .type("S")
                    .build(),
                TableAttributeArgs.builder()
                    .name("sk")
                    .type("S")
                    .build()
            )
            .hashKey("pk")
            .rangeKey("sk")
            .streamEnabled(true)
            .streamViewType("NEW_AND_OLD_IMAGES")
            .tags(java.util.Map.of(
                "Name", tableName,
                "Purpose", "Order storage with PDF generation tracking",
                "Component", "DynamoDBComponent"
            ))
            .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getTableName() {
        return ordersTable.name();
    }

    public Output<String> getTableArn() {
        return ordersTable.arn();
    }

    public Output<String> getStreamArn() {
        return ordersTable.streamArn();
    }

    public Table getTable() {
        return ordersTable;
    }
}