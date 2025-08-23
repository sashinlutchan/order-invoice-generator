package com.orderpdf.app.common.util;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import java.io.InputStream;
import java.io.IOException;

public class S3Helper {
    private final S3Client s3Client;
    private final String bucketName;

    public S3Helper(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void putObjectFromInputStream(String objectKey, InputStream contentStream, String contentType) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType(contentType)
            .build();
        
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(contentStream, contentStream.available()));
    }

    public void putObjectFromBytes(String objectKey, byte[] contentBytes, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType(contentType)
            .build();
        
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(contentBytes));
    }
}