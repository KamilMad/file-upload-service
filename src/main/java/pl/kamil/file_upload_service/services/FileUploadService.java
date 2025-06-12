package pl.kamil.file_upload_service.services;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pl.kamil.file_upload_service.dtos.S3FileResponse;
import pl.kamil.file_upload_service.dtos.UploadUrlResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileUploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RestTemplate restTemplate;

    private final static String bucket = "my-upload-file-bucket-085";

    public FileUploadService(S3Client s3Client, S3Presigner s3Presigner, RestTemplate restTemplate) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.restTemplate = restTemplate;
    }

    public UploadUrlResponse uploadFile(MultipartFile file) {
        String presignedUrl=  generatePostPresignedUrl(file.getOriginalFilename());
        callS3(file, presignedUrl);

        return uploadUrlResponse;
    }

    public void callS3(MultipartFile file, String presignedUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));

        HttpEntity<byte[]> requestEntity;
        try{
            requestEntity= new HttpEntity<>(file.getBytes(), headers);

        }catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }

        ResponseEntity<String> response = restTemplate.exchange(
                presignedUrl,
                HttpMethod.PUT,
                requestEntity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to upload file to S3, status: " + response.getStatusCode());
        }
    }

    public String generatePostPresignedUrl(String filename) {
        try{
            String s3key = UUID.randomUUID() + "-" + filename;
            Duration expiration = Duration.ofMinutes(60);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3key)
                    .build();

            PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(putObjectPresignRequest);

            return presignedPutObjectRequest.url().toString();

        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AWS SDK error while generating presigned URL", e);
        }
    }


    public S3FileResponse getFileResource(String fileKey) {
        ResponseInputStream<GetObjectResponse> s3ObjectStream = getFile(fileKey);

        InputStreamResource resource = new InputStreamResource(s3ObjectStream);
        String contentType = s3ObjectStream.response().contentType();

        return new S3FileResponse(resource, contentType);
    }

    public void deleteByKey(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 error while deleting file", e);
        }

    }

    public ResponseInputStream<GetObjectResponse> getFile(String fileKey) {
        try {
            // Attempt to retrieve the file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey).
                    build();

            return s3Client.getObject(getObjectRequest);

        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileKey);
        }
    }
}
