package com.orderpdf.infrastructure.components;

import com.pulumi.aws.sqs.Queue;
import com.pulumi.aws.sqs.QueueArgs;
import com.pulumi.core.Output;

public class SQSComponent {

    public static class Builder {
        private String queueName;
        private int visibilityTimeoutSeconds = 300;
        private int maxReceiveCount = 5;

        public Builder queueName(String queueName) {
            this.queueName = queueName;
            return this;
        }

        public Builder visibilityTimeoutSeconds(int visibilityTimeoutSeconds) {
            this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
            return this;
        }

        public Builder maxReceiveCount(int maxReceiveCount) {
            this.maxReceiveCount = maxReceiveCount;
            return this;
        }

        public SQSComponent build() {
            return new SQSComponent(queueName, visibilityTimeoutSeconds, maxReceiveCount);
        }
    }

    private final Queue mainQueue;
    private final Queue deadLetterQueue;

    private SQSComponent(String queueName, int visibilityTimeoutSeconds, int maxReceiveCount) {
        this.deadLetterQueue = new Queue(queueName + "-dlq", QueueArgs.builder()
                .name(queueName + "-dlq")
                .tags(java.util.Map.of(
                        "Name", queueName + "-dlq",
                        "Purpose", "Dead letter queue for failed messages",
                        "Component", "SQSComponent"))
                .build());

        this.mainQueue = new Queue(queueName, QueueArgs.builder()
                .name(queueName)
                .visibilityTimeoutSeconds(visibilityTimeoutSeconds)
                .redrivePolicy(deadLetterQueue.arn()
                        .applyValue(dlqArn -> String.format("{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":%d}",
                                dlqArn, maxReceiveCount)))
                .tags(java.util.Map.of(
                        "Name", queueName,
                        "Purpose", "Main queue for order processing",
                        "Component", "SQSComponent"))
                .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getQueueUrl() {
        return mainQueue.url();
    }

    public Output<String> getQueueArn() {
        return mainQueue.arn();
    }

    public Output<String> getDeadLetterQueueUrl() {
        return deadLetterQueue.url();
    }

    public Output<String> getDeadLetterQueueArn() {
        return deadLetterQueue.arn();
    }

    public Queue getMainQueue() {
        return mainQueue;
    }

    public Queue getDeadLetterQueue() {
        return deadLetterQueue;
    }
}