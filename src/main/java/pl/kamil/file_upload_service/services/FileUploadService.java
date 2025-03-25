package pl.kamil.file_upload_service.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.kamil.file_upload_service.dtos.S3FileResponse;
import pl.kamil.file_upload_service.exceptions.FileNotFoundException;
import pl.kamil.file_upload_service.exceptions.FileProcessingException;
import pl.kamil.file_upload_service.exceptions.S3StorageException;
import pl.kamil.file_upload_service.exceptions.StorageServiceException;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileUploadService {

    private final AmazonS3 s3Client;

    private final static String bucket = "my-upload-file-bucket-085";

    public FileUploadService(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    // Return: file url
    public String uploadFile(MultipartFile file) {
        // Prevent filename collisions
        String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        // Prepare metadata for the upload
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());  // Set the file size in metadata

        try {
            // Upload the file to the specific S3 bucket
            s3Client.putObject(new PutObjectRequest(bucket, filename, file.getInputStream(),metadata));

            // Return the file URL from S3 bucket
            return s3Client.getUrl(bucket, filename).toString();


        } catch (AmazonS3Exception e) { // Covers all AWS service errors
            throw new RuntimeException("Error uploading file to S3: " + e.getMessage(), e);

        } catch (SdkClientException e) { // Covers network issues and misconfigured clients
            throw new StorageServiceException("Error communicating with S3: " + e.getMessage(), e);

        } catch (IOException e) { // Handles file read errors
            throw new FileProcessingException("Error reading file for upload: " + e.getMessage(), e);
        }

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
            return s3Client.getObject(new GetObjectRequest(bucket, fileKey));

        } catch (AmazonServiceException e) {
            final int statusCode = e.getStatusCode();

            //Log the error
            System.err.println("S3 error [" + statusCode + "]: " +e.getErrorMessage() );

            // Handle different types of S3 errors explicitly
            if (statusCode == 404) {
                throw new FileNotFoundException("File not found: " + fileKey);
            } else if (statusCode == 403) {
                throw new S3StorageException("Access denied to file: " + fileKey, e);
            }

            // If it's not 404 or 403, treat it as a general S3 storage error
            throw new S3StorageException("S3 error retrieving file: " + fileKey, e);

        } catch (SdkClientException e) {
            // Handle client-side errors
            throw new StorageServiceException("AWS SDK client error", e);
        }

    }

    public void deleteByKey(String key) {
        try {
            if (s3Client.doesObjectExist(bucket, key)) {
                s3Client.deleteObject(bucket, key);
            }
        } catch (AmazonServiceException e) {
            int statusCode = e.getStatusCode();

            System.err.println("S3 error [" + statusCode + "] while deleting: " + e.getErrorMessage());

            if (statusCode == 403) {
                throw new S3StorageException("Access denied when deleting file: " + key, e);
            } else if (statusCode == 404) {
                // You might skip this, since S3 treats deletes as success even if not found
                throw new FileNotFoundException("File not found for deletion: " + key);
            }
        } catch (SdkClientException e) {
            throw new S3StorageException("AWS SDK client error while deleting file: " + key, e);
        }
    }
}
