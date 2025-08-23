package com.orderpdf.infrastructure.lambda;

import com.pulumi.aws.lambda.Function;
import com.pulumi.aws.lambda.FunctionArgs;
import com.pulumi.core.Output;

public class LambdaComponent {

    public static class Builder {
        private String functionName;
        private String handler;
        private String runtime = "java21";
        private Output<String> roleArn;
        private String codeLocation;
        private int memorySize = 512;
        private int timeout = 60;
        private java.util.Map<String, String> environment = java.util.Map.of();

        public Builder functionName(String functionName) {
            this.functionName = functionName;
            return this;
        }

        public Builder handler(String handler) {
            this.handler = handler;
            return this;
        }

        public Builder runtime(String runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder roleArn(Output<String> roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        public Builder codeLocation(String codeLocation) {
            this.codeLocation = codeLocation;
            return this;
        }

        public Builder memorySize(int memorySize) {
            this.memorySize = memorySize;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder environment(java.util.Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public LambdaComponent build() {
            return new LambdaComponent(functionName, handler, runtime, roleArn, codeLocation,
                    memorySize, timeout, environment);
        }
    }

    private final Function lambdaFunction;

    private LambdaComponent(String functionName, String handler, String runtime, Output<String> roleArn,
            String codeLocation, int memorySize, int timeout,
            java.util.Map<String, String> environment) {
        this.lambdaFunction = new Function(functionName, FunctionArgs.builder()
                .name(functionName)
                .handler(handler)
                .runtime(runtime)
                .role(roleArn)
                .code(new com.pulumi.asset.FileArchive(codeLocation))
                .memorySize(memorySize)
                .timeout(timeout)
                .environment(com.pulumi.aws.lambda.inputs.FunctionEnvironmentArgs.builder()
                        .variables(environment)
                        .build())
                .tags(java.util.Map.of(
                        "Name", functionName,
                        "Purpose", "Order processing Lambda function",
                        "Component", "LambdaComponent"))
                .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getFunctionArn() {
        return lambdaFunction.arn();
    }

    public Output<String> getFunctionName() {
        return lambdaFunction.name();
    }

    public Function getFunction() {
        return lambdaFunction;
    }
}