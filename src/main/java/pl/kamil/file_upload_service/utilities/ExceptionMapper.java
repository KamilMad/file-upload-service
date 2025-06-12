package pl.kamil.file_upload_service.utilities;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

public class ExceptionMapper {

    public static ResponseStatusException mapS3DeleteException(Exception e, String key) {
        if (e instanceof NoSuchKeyException){
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + key, e);
        } else if (e instanceof S3Exception s3Exception) {
            int statusCode = s3Exception.statusCode();

            if (statusCode == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to file: " + key, e);
            }else if (statusCode == 404) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to file: " + key, e);
            }

        } else if (e instanceof SdkClientException){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AWS SDK client error: " + e.getMessage(), e);
        } else if(e instanceof AwsServiceException awse){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AWS service error: " + awse.awsErrorDetails().errorMessage(), e);
        }
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error deleting file: " + key, e);
    }

    public static ResponseStatusException mapS3PutException(Exception e) {
        if (e instanceof S3Exception) { // Covers all AWS service errors
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 service error: " + e.getMessage(), e);
        } else if (e instanceof SdkClientException) { // Covers network issues and misconfigured clients
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 client/network error: " + e.getMessage(), e);
        }else if(e instanceof IOException) { // Handles file read errors
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file data", e);
        }

        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error putting file", e);

    }
}
