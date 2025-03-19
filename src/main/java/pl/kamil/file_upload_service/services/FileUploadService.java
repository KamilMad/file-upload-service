package pl.kamil.file_upload_service.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
            // Get the object from the S3 bucket using the key (file name)
            return s3Client.getObject(new GetObjectRequest(bucket, fileKey));
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new FileNotFoundException("File not found: " + fileKey);
            }
            throw new S3StorageException("S3 error retrieving file: " + fileKey, e);
        } catch (SdkClientException e) {
            throw new StorageServiceException("AWS SDK client error", e);
        }

    }
}
