package com.orderpdf.infrastructure.stepfunctions;

import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicy;
import com.pulumi.aws.iam.RolePolicyArgs;
import com.pulumi.aws.sfn.StateMachine;
import com.pulumi.aws.sfn.StateMachineArgs;
import com.pulumi.core.Output;

public class StepFunctionsComponent {

    public static class Builder {
        private String stateMachineName;
        private Output<String> generatePdfLambdaArn;
        private Output<String> bucketArn;
        private Output<String> tableArn;

        public Builder stateMachineName(String stateMachineName) {
            this.stateMachineName = stateMachineName;
            return this;
        }

        public Builder generatePdfLambdaArn(Output<String> generatePdfLambdaArn) {
            this.generatePdfLambdaArn = generatePdfLambdaArn;
            return this;
        }

        public Builder bucketArn(Output<String> bucketArn) {
            this.bucketArn = bucketArn;
            return this;
        }

        public Builder tableArn(Output<String> tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        public StepFunctionsComponent build() {
            return new StepFunctionsComponent(stateMachineName, generatePdfLambdaArn,
                    bucketArn, tableArn);
        }
    }

    private final StateMachine stateMachine;
    private final Role stateMachineRole;

    private StepFunctionsComponent(String stateMachineName, Output<String> generatePdfLambdaArn,
            Output<String> bucketArn, Output<String> tableArn) {
        String assumeRolePolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {
                                "Service": "states.amazonaws.com"
                            },
                            "Action": "sts:AssumeRole"
                        }
                    ]
                }
                """;

        this.stateMachineRole = new Role(stateMachineName + "-role", RoleArgs.builder()
                .name(stateMachineName + "-role")
                .assumeRolePolicy(assumeRolePolicy)
                .tags(java.util.Map.of(
                        "Name", stateMachineName + "-role",
                        "Purpose", "Step Functions execution role",
                        "Component", "StepFunctionsComponent"))
                .build());

        Output<String> policyDocument = Output.all(generatePdfLambdaArn, bucketArn, tableArn)
                .apply(values -> Output.of(String.format("""
                        {
                            "Version": "2012-10-17",
                            "Statement": [
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "lambda:InvokeFunction"
                                    ],
                                    "Resource": "%s"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "s3:GetObject",
                                        "s3:PutObject"
                                    ],
                                    "Resource": "%s/*"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "dynamodb:GetItem",
                                        "dynamodb:PutItem",
                                        "dynamodb:UpdateItem"
                                    ],
                                    "Resource": "%s"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "pipes:PutEvents",
                                        "pipes:DescribePipe",
                                        "pipes:ListPipes"
                                    ],
                                    "Resource": "*"
                                }
                            ]
                        }
                        """, values.get(0), values.get(1), values.get(2))));

        new RolePolicy(stateMachineName + "-policy", RolePolicyArgs.builder()
                .name(stateMachineName + "-policy")
                .role(stateMachineRole.id())
                .policy(policyDocument)
                .build());

        Output<String> stateMachineDefinition = generatePdfLambdaArn
                .apply(arn -> Output.of(String.format(
                        """
                                {
                                    "Comment": "Order PDF Generation State Machine - Process SQS messages from EventBridge Pipes",
                                    "StartAt": "ProcessSQSMessages",
                                    "States": {
                                        "ProcessSQSMessages": {
                                            "Type": "Map",
                                            "ItemsPath": "$",
                                            "MaxConcurrency": 10,
                                            "Iterator": {
                                                "StartAt": "ParseSQSMessage",
                                                "States": {
                                                    "ParseSQSMessage": {
                                                        "Type": "Pass",
                                                        "Parameters": {
                                                            "dynamoRecord.$": "States.StringToJson($.body)"
                                                        },
                                                        "Next": "CheckEventType"
                                                    },
                                                    "CheckEventType": {
                                                        "Type": "Choice",
                                                        "Choices": [
                                                            {
                                                                "And": [
                                                                    {
                                                                        "Variable": "$.dynamoRecord.eventName",
                                                                        "StringEquals": "INSERT"
                                                                    },
                                                                    {
                                                                        "Variable": "$.dynamoRecord.dynamodb.Keys.sk.S",
                                                                        "StringEquals": "STATE#v1"
                                                                    }
                                                                ],
                                                                "Next": "InvokePDFGeneratorLambda"
                                                            }
                                                        ],
                                                        "Default": "SkipEvent"
                                                    },
                                                    "InvokePDFGeneratorLambda": {
                                                        "Type": "Task",
                                                        "Resource": "%s",
                                                        "Parameters": {
                                                            "pk.$": "$.dynamoRecord.dynamodb.Keys.pk.S",
                                                            "sk.$": "$.dynamoRecord.dynamodb.Keys.sk.S",
                                                            "orderId.$": "$.dynamoRecord.dynamodb.NewImage.orderId.S",
                                                            "oldPdfKey": null
                                                        },
                                                        "ResultPath": "$.pdfResult",
                                                        "Retry": [
                                                            {
                                                                "ErrorEquals": ["Lambda.TooManyRequestsException"],
                                                                "IntervalSeconds": 5,
                                                                "MaxAttempts": 20,
                                                                "BackoffRate": 2.0,
                                                                "JitterStrategy": "FULL"
                                                            }
                                                        ],
                                                        "End": true
                                                    },
                                                    "SkipEvent": {
                                                        "Type": "Pass",
                                                        "Result": "Skipped non-order event",
                                                        "End": true
                                                    }
                                                }
                                            },
                                            "End": true
                                        }
                                    }
                                }
                                """,
                        arn)));

        this.stateMachine = new StateMachine(stateMachineName, StateMachineArgs.builder()
                .name(stateMachineName)
                .roleArn(stateMachineRole.arn())
                .definition(stateMachineDefinition)
                .tags(java.util.Map.of(
                        "Name", stateMachineName,
                        "Purpose", "Order PDF generation workflow",
                        "Component", "StepFunctionsComponent"))
                .build());

    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getStateMachineArn() {
        return stateMachine.arn();
    }

    public Output<String> getStateMachineName() {
        return stateMachine.name();
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public Role getStateMachineRole() {
        return stateMachineRole;
    }
}