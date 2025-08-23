package com.orderpdf.infrastructure.roles;

import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicy;
import com.pulumi.aws.iam.RolePolicyArgs;
import com.pulumi.core.Output;

public class IAMRolesComponent {

    public static class Builder {
        private Output<String> tableArn;
        private Output<String> streamArn;
        private Output<String> queueArn;
        private Output<String> bucketArn;

        public Builder tableArn(Output<String> tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        public Builder streamArn(Output<String> streamArn) {
            this.streamArn = streamArn;
            return this;
        }

        public Builder queueArn(Output<String> queueArn) {
            this.queueArn = queueArn;
            return this;
        }

        public Builder bucketArn(Output<String> bucketArn) {
            this.bucketArn = bucketArn;
            return this;
        }

        public IAMRolesComponent build() {
            return new IAMRolesComponent(tableArn, streamArn, queueArn, bucketArn);
        }
    }

    private final Role preprocessHandlerRole;
    private final Role generatePdfHandlerRole;

    private IAMRolesComponent(Output<String> tableArn, Output<String> streamArn, Output<String> queueArn,
            Output<String> bucketArn) {
        String lambdaAssumeRolePolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {
                                "Service": "lambda.amazonaws.com"
                            },
                            "Action": "sts:AssumeRole"
                        }
                    ]
                }
                """;

        this.preprocessHandlerRole = new Role("preprocess-handler-role", RoleArgs.builder()
                .name("order-generator-preprocess-handler-role")
                .assumeRolePolicy(lambdaAssumeRolePolicy)
                .tags(java.util.Map.of(
                        "Name", "order-generator-preprocess-handler-role",
                        "Purpose", "Preprocess Lambda execution role",
                        "Component", "IAMRolesComponent"))
                .build());

        Output<String> preprocessPolicyDocument = Output.all(tableArn, queueArn)
                .apply(values -> Output.of(String.format("""
                        {
                            "Version": "2012-10-17",
                            "Statement": [
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "dynamodb:GetItem",
                                        "dynamodb:PutItem",
                                        "dynamodb:UpdateItem",
                                        "dynamodb:DeleteItem",
                                        "dynamodb:Query",
                                        "dynamodb:Scan"
                                    ],
                                    "Resource": "%s"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "sqs:SendMessage",
                                        "sqs:DeleteMessage"
                                    ],
                                    "Resource": "%s"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "logs:CreateLogGroup",
                                        "logs:CreateLogStream",
                                        "logs:PutLogEvents"
                                    ],
                                    "Resource": "arn:aws:logs:*:*:*"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "kms:Decrypt",
                                        "kms:DescribeKey"
                                    ],
                                    "Resource": "*"
                                }
                            ]
                        }
                        """, values.get(0), values.get(1))));

        new RolePolicy("preprocess-policy", RolePolicyArgs.builder()
                .name("order-generator-preprocess-policy")
                .role(preprocessHandlerRole.id())
                .policy(preprocessPolicyDocument)
                .build());

        this.generatePdfHandlerRole = new Role("generate-pdf-handler-role", RoleArgs.builder()
                .name("order-generator-generate-pdf-handler-role")
                .assumeRolePolicy(lambdaAssumeRolePolicy)
                .tags(java.util.Map.of(
                        "Name", "order-generator-generate-pdf-handler-role",
                        "Purpose", "Generate PDF Lambda execution role",
                        "Component", "IAMRolesComponent"))
                .build());

        Output<String> generatePdfPolicyDocument = Output.all(tableArn, bucketArn)
                .apply(values -> Output.of(String.format("""
                        {
                            "Version": "2012-10-17",
                            "Statement": [
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "dynamodb:GetItem",
                                        "dynamodb:Query"
                                    ],
                                    "Resource": "%s"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "s3:PutObject",
                                        "s3:GetObject",
                                        "s3:DeleteObject"
                                    ],
                                    "Resource": "%s/*"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "logs:CreateLogGroup",
                                        "logs:CreateLogStream",
                                        "logs:PutLogEvents"
                                    ],
                                    "Resource": "arn:aws:logs:*:*:*"
                                },
                                {
                                    "Effect": "Allow",
                                    "Action": [
                                        "kms:Decrypt",
                                        "kms:DescribeKey"
                                    ],
                                    "Resource": "*"
                                }
                            ]
                        }
                        """, values.get(0), values.get(1))));

        new RolePolicy("generate-pdf-policy", RolePolicyArgs.builder()
                .name("order-generator-generate-pdf-policy")
                .role(generatePdfHandlerRole.id())
                .policy(generatePdfPolicyDocument)
                .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getPreprocessHandlerRoleArn() {
        return preprocessHandlerRole.arn();
    }

    public Output<String> getGeneratePdfHandlerRoleArn() {
        return generatePdfHandlerRole.arn();
    }

    public Role getPreprocessHandlerRole() {
        return preprocessHandlerRole;
    }

    public Role getGeneratePdfHandlerRole() {
        return generatePdfHandlerRole;
    }
}