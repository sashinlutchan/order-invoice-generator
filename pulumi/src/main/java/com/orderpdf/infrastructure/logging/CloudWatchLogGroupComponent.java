package com.orderpdf.infrastructure.logging;

import com.pulumi.aws.cloudwatch.LogGroup;
import com.pulumi.aws.cloudwatch.LogGroupArgs;
import com.pulumi.core.Output;

public class CloudWatchLogGroupComponent {

    public static class Builder {
        private String logGroupName;
        private Integer retentionInDays;

        public Builder logGroupName(String logGroupName) {
            this.logGroupName = logGroupName;
            return this;
        }

        public Builder retentionInDays(Integer retentionInDays) {
            this.retentionInDays = retentionInDays;
            return this;
        }

        public CloudWatchLogGroupComponent build() {
            return new CloudWatchLogGroupComponent(logGroupName, retentionInDays);
        }
    }

    private final LogGroup logGroup;

    private CloudWatchLogGroupComponent(String logGroupName, Integer retentionInDays) {
        this.logGroup = new LogGroup("pipes-log-group", LogGroupArgs.builder()
                .name(logGroupName)
                .retentionInDays(retentionInDays != null ? retentionInDays : 7)
                .tags(java.util.Map.of(
                        "Name", logGroupName,
                        "Purpose", "EventBridge Pipes logging",
                        "Component", "CloudWatchLogGroupComponent"))
                .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getLogGroupArn() {
        return logGroup.arn();
    }

    public Output<String> getLogGroupName() {
        return logGroup.name();
    }

    public LogGroup getLogGroup() {
        return logGroup;
    }
}