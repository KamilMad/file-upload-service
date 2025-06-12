package pl.kamil.file_upload_service.dtos;

import java.time.Duration;

public record UploadUrlResponse (String presignedUrl, String s3key, long expiration) implements UrlResponse{
}
