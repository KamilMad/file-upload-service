package pl.kamil.file_upload_service.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pl.kamil.file_upload_service.dtos.FileMetadataResponse;
import pl.kamil.file_upload_service.dtos.S3FileResponse;
import pl.kamil.file_upload_service.models.FileMetadata;
import pl.kamil.file_upload_service.repositories.FileMetadataRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileUploadService {

    private final AmazonS3 s3Client;
    private final FileMetadataRepository fileMetadataRepository;

    private final static String bucket = "my-upload-file-bucket-085";

    public FileUploadService(AmazonS3 s3Client, FileMetadataRepository fileMetadataRepository) {
        this.s3Client = s3Client;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    // Return: file url
    public String uploadFile(MultipartFile file) {
        // Prevent filename collisions
        String s3key = UUID.randomUUID() + "-" + file.getOriginalFilename();

        // Prepare metadata for the upload
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("original-filename", file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());  // Set the file size in metadata

        try(InputStream inputStream = file.getInputStream()) {
            // Upload the file to the specific S3 bucket
            PutObjectRequest request = new PutObjectRequest(bucket, s3key, inputStream, metadata);
            s3Client.putObject(request);

            // Return the file URL from S3 bucket
            //return s3Client.getUrl(bucket, s3key).toString();
            return s3key;

        } catch (AmazonS3Exception e) { // Covers all AWS service errors
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 service error: " + e.getErrorMessage(), e);
        } catch (SdkClientException e) { // Covers network issues and misconfigured clients
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 client/network error: " + e.getMessage(), e);
        } catch (IOException e) { // Handles file read errors
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file data", e);
        }

    }

    public FileMetadataResponse UploadFileWithMetadata(MultipartFile file, Long lessonId, Long userId) {
        // It returns url but should return s3 key
        String s3Key = uploadFile(file);

        FileMetadata fileMetadata =  createFileMetadata(file, s3Key, lessonId, userId);
        fileMetadataRepository.save(fileMetadata);

        return mapFileMetadataToFileMetadataResponse(fileMetadata);
    }

    public FileMetadata createFileMetadata(MultipartFile file, String s3key, Long lessonId, long userId) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setOriginalName(file.getOriginalFilename());
        fileMetadata.setContentType(file.getContentType());
        fileMetadata.setSize(file.getSize());
        fileMetadata.setS3Key(s3key);
        fileMetadata.setUploadedBy(userId);
        fileMetadata.setLessonId(lessonId);
        return fileMetadata;
    }
    public S3FileResponse getFileResource(String fileKey) {
        S3Object s3Object = getFile(fileKey);
        InputStreamResource resource = new InputStreamResource(s3Object.getObjectContent());
        String contentType = s3Object.getObjectMetadata().getContentType();

        return new S3FileResponse(resource, contentType);
    }

    public S3Object getFile(String fileKey) {
        try {
            // Attempt to retrieve the file from S3

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, fileKey);
            return s3Client.getObject(getObjectRequest);

        } catch (AmazonServiceException e) {
            final int statusCode = e.getStatusCode();

            //Log the error
            System.err.println("S3 error [" + statusCode + "]: " +e.getErrorMessage() );

            switch (statusCode) {
                case 404 -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileKey);
                case 403 -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to file: " + fileKey, e);
                // If it's not 404 or 403, treat it as a general S3 storage error
                default -> throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 error: " + e.getErrorMessage(), e);
            }

        } catch (SdkClientException e) {
            // Handle client-side errors
            System.err.println("S3 client error while retrieving '" + fileKey + "': " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AWS SDK client error", e);
        }

    }

    public void deleteByKey(String key) {
        try {
            s3Client.deleteObject(bucket, key);

        } catch (AmazonServiceException e) {
            int statusCode = e.getStatusCode();
            System.err.println("S3 error [" + statusCode + "] while deleting: " + e.getErrorMessage());

            if (statusCode == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to file: " + key, e);
            }

        } catch (SdkClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 error while deleting file", e);
        }
    }

    public String getFileUrl(String s3Key) {
        return s3Client.getUrl(bucket, s3Key).toString();
    }

    public FileMetadataResponse mapFileMetadataToFileMetadataResponse(FileMetadata fileMetadata) {
        String s3Key =  getFileUrl(fileMetadata.getS3Key());
        return new FileMetadataResponse(fileMetadata.getId(),
                        fileMetadata.getOriginalName(),
                        fileMetadata.getContentType(),
                        fileMetadata.getSize(),
                        s3Key);
    }
}
