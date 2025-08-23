package com.orderpdf.infrastructure;

import com.pulumi.Pulumi;
import com.orderpdf.infrastructure.components.DynamoDBComponent;
import com.orderpdf.infrastructure.components.SQSComponent;
import com.orderpdf.infrastructure.storage.S3Component;
import com.orderpdf.infrastructure.roles.IAMRolesComponent;
import com.orderpdf.infrastructure.lambda.LambdaComponent;
import com.orderpdf.infrastructure.stepfunctions.StepFunctionsComponent;
import com.orderpdf.infrastructure.eventbridge.EventBridgePipesComponent;
import com.orderpdf.infrastructure.roles.PipesIAMRoleComponent;
import com.orderpdf.infrastructure.logging.CloudWatchLogGroupComponent;
import com.orderpdf.infrastructure.helpers.LambaBuilder;

public class PulumiStack {
        public static void main(String[] args) {
                Pulumi.run(context -> {

                        var config = context.config();

                        String tableName = config.require("tableName");
                        String bucketName = config.require("bucketName");
                        String queueName = "orders-queue";
                        String reprocessPolicy = config.get("reprocessPolicy").orElse("FIRST_TIME_ONLY");

                        DynamoDBComponent dynamoDBComponent = DynamoDBComponent.builder()
                                        .tableName(tableName)
                                        .build();

                        S3Component s3Component = S3Component.builder()
                                        .bucketName(bucketName)
                                        .build();

                        SQSComponent sqsComponent = SQSComponent.builder()
                                        .queueName(queueName)
                                        .visibilityTimeoutSeconds(600)
                                        .maxReceiveCount(5)
                                        .build();

                        IAMRolesComponent iamRolesComponent = IAMRolesComponent.builder()
                                        .tableArn(dynamoDBComponent.getTableArn())
                                        .streamArn(dynamoDBComponent.getStreamArn())
                                        .queueArn(sqsComponent.getQueueArn())
                                        .bucketArn(s3Component.getBucketArn())
                                        .build();

                        String buildApi = LambaBuilder.Build();
                        LambdaComponent preprocessHandler = LambdaComponent.builder()
                                        .functionName("order-generator-preprocess-handler")
                                        .handler("com.orderpdf.app.preprocess.PreprocessHandler::handleRequest")
                                        .roleArn(iamRolesComponent.getPreprocessHandlerRoleArn())
                                        .codeLocation(buildApi)
                                        .environment(java.util.Map.of(
                                                        "REPROCESS_POLICY", reprocessPolicy))
                                        .memorySize(512)
                                        .timeout(60)
                                        .build();

                        LambdaComponent generatePdfHandler = LambdaComponent.builder()
                                        .functionName("order-generator-generate-pdf-handler")
                                        .handler("com.orderpdf.app.pdf.GeneratePdfHandler::handleRequest")
                                        .roleArn(iamRolesComponent.getGeneratePdfHandlerRoleArn())
                                        .codeLocation(buildApi)
                                        .environment(java.util.Map.of(
                                                        "BUCKET_NAME", bucketName))
                                        .memorySize(1024)
                                        .timeout(120)
                                        .build();

                        StepFunctionsComponent stepFunctionsComponent = StepFunctionsComponent.builder()
                                        .stateMachineName("order-generator-processor")
                                        .generatePdfLambdaArn(generatePdfHandler.getFunctionArn())
                                        .bucketArn(s3Component.getBucketArn())
                                        .tableArn(dynamoDBComponent.getTableArn())
                                        .build();

                        CloudWatchLogGroupComponent logGroupComponent = CloudWatchLogGroupComponent.builder()
                                        .logGroupName("order-generator-pipes-logs")
                                        .retentionInDays(7)
                                        .build();

                        PipesIAMRoleComponent pipesIAMRoleComponent = PipesIAMRoleComponent.builder()
                                        .streamArn(dynamoDBComponent.getStreamArn())
                                        .queueArn(sqsComponent.getQueueArn())
                                        .stepFunctionArn(stepFunctionsComponent.getStateMachineArn())
                                        .build();

                        EventBridgePipesComponent eventBridgePipesComponent = EventBridgePipesComponent.builder()
                                        .streamArn(dynamoDBComponent.getStreamArn())
                                        .queueArn(sqsComponent.getQueueArn())
                                        .pipesRoleArn(pipesIAMRoleComponent.getPipesRoleArn())
                                        .preprocessHandlerArn(preprocessHandler.getFunctionArn())
                                        .stepFunctionArn(stepFunctionsComponent.getStateMachineArn())
                                        .logGroupArn(logGroupComponent.getLogGroupArn())
                                        .build();

                        context.export("ordersTableName", dynamoDBComponent.getTableName());
                        context.export("ordersTableArn", dynamoDBComponent.getTableArn());
                        context.export("bucketName", s3Component.getBucketName());
                        context.export("bucketArn", s3Component.getBucketArn());
                        context.export("queueUrl", sqsComponent.getQueueUrl());
                        context.export("queueArn", sqsComponent.getQueueArn());
                        context.export("deadLetterQueueUrl", sqsComponent.getDeadLetterQueueUrl());
                        context.export("deadLetterQueueArn", sqsComponent.getDeadLetterQueueArn());
                        context.export("preprocessHandlerArn", preprocessHandler.getFunctionArn());
                        context.export("generatePdfHandlerArn", generatePdfHandler.getFunctionArn());
                        context.export("stateMachineArn", stepFunctionsComponent.getStateMachineArn());
                        context.export("pipeAArn", eventBridgePipesComponent.getPipeAArn());
                        context.export("pipeBArn", eventBridgePipesComponent.getPipeBArn());
                });
        }

}