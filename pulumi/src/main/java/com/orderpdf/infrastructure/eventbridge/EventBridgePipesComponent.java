package com.orderpdf.infrastructure.eventbridge;

import com.pulumi.aws.pipes.Pipe;
import com.pulumi.aws.pipes.PipeArgs;
import com.pulumi.aws.pipes.inputs.*;
import com.pulumi.core.Output;

import java.util.Map;

public class EventBridgePipesComponent {

        public static class Builder {
                private Output<String> streamArn;
                private Output<String> queueArn;
                private Output<String> pipesRoleArn;
                private Output<String> preprocessHandlerArn;
                private Output<String> stepFunctionArn;
                private Output<String> logGroupArn;

                public Builder streamArn(Output<String> streamArn) {
                        this.streamArn = streamArn;
                        return this;
                }

                public Builder queueArn(Output<String> queueArn) {
                        this.queueArn = queueArn;
                        return this;
                }

                public Builder pipesRoleArn(Output<String> pipesRoleArn) {
                        this.pipesRoleArn = pipesRoleArn;
                        return this;
                }

                public Builder preprocessHandlerArn(Output<String> preprocessHandlerArn) {
                        this.preprocessHandlerArn = preprocessHandlerArn;
                        return this;
                }

                public Builder stepFunctionArn(Output<String> stepFunctionArn) {
                        this.stepFunctionArn = stepFunctionArn;
                        return this;
                }

                public Builder logGroupArn(Output<String> logGroupArn) {
                        this.logGroupArn = logGroupArn;
                        return this;
                }

                public EventBridgePipesComponent build() {
                        return new EventBridgePipesComponent(streamArn, queueArn, pipesRoleArn,
                                        preprocessHandlerArn, stepFunctionArn, logGroupArn);
                }
        }

        private final Pipe pipeA;
        private final Pipe pipeB;

        private EventBridgePipesComponent(Output<String> streamArn, Output<String> queueArn,
                        Output<String> pipesRoleArn,
                        Output<String> preprocessHandlerArn, Output<String> stepFunctionArn,
                        Output<String> logGroupArn) {
                this.pipeA = new Pipe("pipe-a-dynamodb-to-sqs", PipeArgs.builder()
                                .name("order-generator-pipe-a-dynamodb-to-sqs")
                                .description("Pipe A: DynamoDB Stream to SQS")
                                .roleArn(pipesRoleArn)
                                .source(streamArn)
                                .target(queueArn)
                                .sourceParameters(PipeSourceParametersArgs.builder()
                                                .dynamodbStreamParameters(
                                                                PipeSourceParametersDynamodbStreamParametersArgs
                                                                                .builder()
                                                                                .startingPosition("LATEST")
                                                                                .batchSize(5)
                                                                                .maximumBatchingWindowInSeconds(30)
                                                                                .maximumRecordAgeInSeconds(120)
                                                                                .parallelizationFactor(1)
                                                                                .build())
                                                .build())
                                .logConfiguration(PipeLogConfigurationArgs.builder()
                                                .includeExecutionDatas("ALL")
                                                .level("INFO")
                                                .cloudwatchLogsLogDestination(
                                                                PipeLogConfigurationCloudwatchLogsLogDestinationArgs
                                                                                .builder()
                                                                                .logGroupArn(logGroupArn)
                                                                                .build())
                                                .build())
                                .tags(java.util.Map.of(
                                                "Name", "order-generator-pipe-a-dynamodb-to-sqs",
                                                "Purpose", "Stream DynamoDB changes to SQS queue",
                                                "Component", "EventBridgePipesComponent"))
                                .build());

                this.pipeB = new Pipe("pipe-b-sqs-to-stepfunctions", PipeArgs.builder()
                                .name("order-generator-pipe-b-sqs-to-stepfunctions")
                                .description("Pipe B: SQS to Step Functions")
                                .roleArn(pipesRoleArn)
                                .source(queueArn)
                                .target(stepFunctionArn)
                                .sourceParameters(PipeSourceParametersArgs.builder()
                                                .sqsQueueParameters(PipeSourceParametersSqsQueueParametersArgs.builder()
                                                                .batchSize(3)
                                                                .maximumBatchingWindowInSeconds(60)
                                                                .build())
                                                .build())
                                .targetParameters(PipeTargetParametersArgs.builder()
                                                .stepFunctionStateMachineParameters(
                                                                PipeTargetParametersStepFunctionStateMachineParametersArgs
                                                                                .builder()
                                                                                .invocationType("FIRE_AND_FORGET")
                                                                                .build())
                                                .build())
                                .logConfiguration(PipeLogConfigurationArgs.builder()
                                                .includeExecutionDatas("ALL")
                                                .level("INFO")
                                                .cloudwatchLogsLogDestination(
                                                                PipeLogConfigurationCloudwatchLogsLogDestinationArgs
                                                                                .builder()
                                                                                .logGroupArn(logGroupArn)
                                                                                .build())
                                                .build())
                                .tags(java.util.Map.of(
                                                "Name", "order-generator-pipe-b-sqs-to-stepfunctions",
                                                "Purpose", "Process SQS messages to Step Functions",
                                                "Component", "EventBridgePipesComponent"))
                                .build());
        }

        public static Builder builder() {
                return new Builder();
        }

        public Output<String> getPipeAArn() {
                return pipeA.arn();
        }

        public Output<String> getPipeBArn() {
                return pipeB.arn();
        }

        public Pipe getPipeA() {
                return pipeA;
        }

        public Pipe getPipeB() {
                return pipeB;
        }
}