package com.orderpdf.infrastructure.roles;

import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicy;
import com.pulumi.aws.iam.RolePolicyArgs;
import com.pulumi.core.Output;

public class PipesIAMRoleComponent {

    public static class Builder {
        private Output<String> streamArn;
        private Output<String> queueArn;
        private Output<String> stepFunctionArn;

        public Builder streamArn(Output<String> streamArn) {
            this.streamArn = streamArn;
            return this;
        }

        public Builder queueArn(Output<String> queueArn) {
            this.queueArn = queueArn;
            return this;
        }

        public Builder stepFunctionArn(Output<String> stepFunctionArn) {
            this.stepFunctionArn = stepFunctionArn;
            return this;
        }

        public PipesIAMRoleComponent build() {
            return new PipesIAMRoleComponent(streamArn, queueArn, stepFunctionArn);
        }
    }

    private final Role pipesRole;

    private PipesIAMRoleComponent(Output<String> streamArn, Output<String> queueArn, Output<String> stepFunctionArn) {
        String assumeRolePolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {
                                "Service": "pipes.amazonaws.com"
                            },
                            "Action": "sts:AssumeRole"
                        }
                    ]
                }
                """;

        this.pipesRole = new Role("pipes-role", RoleArgs.builder()
                .name("order-generator-pipes-role")
                .assumeRolePolicy(assumeRolePolicy)
                .tags(java.util.Map.of(
                        "Name", "order-generator-pipes-role",
                        "Purpose", "EventBridge Pipes execution role",
                        "Component", "PipesIAMRoleComponent"))
                .build());

        Output<String> pipesPolicyDocument;

        if (stepFunctionArn != null) {
            pipesPolicyDocument = Output.all(streamArn, queueArn, stepFunctionArn)
                    .apply(values -> Output.of(String.format("""
                            {
                                "Version": "2012-10-17",
                                "Statement": [
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "dynamodb:DescribeStream",
                                            "dynamodb:GetRecords",
                                            "dynamodb:GetShardIterator",
                                            "dynamodb:ListStreams"
                                        ],
                                        "Resource": "%s"
                                    },
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "sqs:SendMessage",
                                            "sqs:ReceiveMessage",
                                            "sqs:DeleteMessage",
                                            "sqs:GetQueueAttributes"
                                        ],
                                        "Resource": "%s"
                                    },
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "states:StartExecution",
                                            "states:SendTaskSuccess",
                                            "states:SendTaskFailure"
                                        ],
                                        "Resource": "%s"
                                    },
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "states:DescribeStateMachine",
                                            "states:DescribeExecution",
                                            "states:StopExecution"
                                        ],
                                        "Resource": "%s"
                                    },
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "logs:CreateLogGroup",
                                            "logs:CreateLogStream",
                                            "logs:PutLogEvents",
                                            "logs:DescribeLogGroups",
                                            "logs:DescribeLogStreams"
                                        ],
                                        "Resource": "arn:aws:logs:*:*:*"
                                    }
                                ]
                            }
                            """, values.get(0), values.get(1), values.get(2), values.get(2))));
        } else {
            pipesPolicyDocument = Output.all(streamArn, queueArn)
                    .apply(values -> Output.of(String.format("""
                            {
                                "Version": "2012-10-17",
                                "Statement": [
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "dynamodb:DescribeStream",
                                            "dynamodb:GetRecords",
                                            "dynamodb:GetShardIterator",
                                            "dynamodb:ListStreams"
                                        ],
                                        "Resource": "%s"
                                    },
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "sqs:SendMessage",
                                            "sqs:ReceiveMessage",
                                            "sqs:DeleteMessage",
                                            "sqs:GetQueueAttributes"
                                        ],
                                        "Resource": "%s"
                                    },
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "logs:CreateLogGroup",
                                            "logs:CreateLogStream",
                                            "logs:PutLogEvents",
                                            "logs:DescribeLogGroups",
                                            "logs:DescribeLogStreams"
                                        ],
                                        "Resource": "arn:aws:logs:*:*:*"
                                    },
                                    {
                                        "Effect": "Allow",
                                        "Action": [
                                            "dynamodb:DescribeTable"
                                        ],
                                        "Resource": "arn:aws:dynamodb:*:*:table/*"
                                    }
                                ]
                            }
                            """, values.get(0), values.get(1))));
        }

        new RolePolicy("pipes-policy", RolePolicyArgs.builder()
                .name("order-generator-pipes-policy")
                .role(pipesRole.id())
                .policy(pipesPolicyDocument)
                .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getPipesRoleArn() {
        return pipesRole.arn();
    }

    public Role getPipesRole() {
        return pipesRole;
    }
}