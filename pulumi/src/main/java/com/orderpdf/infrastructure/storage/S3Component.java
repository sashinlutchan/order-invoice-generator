package com.orderpdf.infrastructure.storage;

import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.s3.BucketArgs;
import com.pulumi.core.Output;

public class S3Component {
    
    public static class Builder {
        private String bucketName;

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public S3Component build() {
            return new S3Component(bucketName);
        }
    }

    private final Bucket bucket;

    private S3Component(String bucketName) {
        this.bucket = new Bucket(bucketName, BucketArgs.builder()
            .bucket(bucketName)
            .tags(java.util.Map.of(
                "Name", bucketName,
                "Purpose", "PDF storage and temporary files",
                "Component", "S3Component"
            ))
            .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Output<String> getBucketName() {
        return bucket.bucket();
    }

    public Output<String> getBucketArn() {
        return bucket.arn();
    }

    public Bucket getBucket() {
        return bucket;
    }
}