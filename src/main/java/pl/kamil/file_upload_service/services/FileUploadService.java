package pl.kamil.file_upload_service.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.kamil.file_upload_service.exceptions.FileNotFoundException;
import pl.kamil.file_upload_service.exceptions.S3StorageException;
import pl.kamil.file_upload_service.exceptions.StorageServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.UUID;

@Service
public class FileUploadService {

    private final AmazonS3 s3Client;

    private final static String bucket = "my-upload-file-bucket-085";

    public FileUploadService(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }


    // return: file url
    public String uploadFile(MultipartFile file) {
        //prevent filename collisions
        String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        // Prepare metadata for the upload
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());  // Set the file size in metadata

        try {
            // upload the file to the specific S3 bucket
            s3Client.putObject(new PutObjectRequest(bucket, filename, file.getInputStream(),metadata));
            // Return the file URL from S3 bucket
            return s3Client.getUrl(bucket, filename).toString();
        } catch (AmazonServiceException e) {
            throw new RuntimeException("Error uploading file to S3: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new RuntimeException("Error communicating with S3: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file for upload: " + e.getMessage(), e);
        }

    }

    public S3Object getFile(String fileKey) {
        try {
            // Attempt to retrieve the file from S3
            return s3Client.getObject(new GetObjectRequest(bucket, fileKey));

        } catch (AmazonS3Exception e) {
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
}
