package pl.kamil.file_upload_service.services;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileUploadService {

    private final AmazonS3 s3Client;

    public FileUploadService(AmazonS3 s3Client) {
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.EU_CENTRAL_1)
                .build();
    }

    /*
    return: file url
     */
    public String uploadFile(MultipartFile file) {
        //prevent filename collisions
        String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        // Prepare metadata for the upload
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());  // Set the file size in metadata

        try {
            // upload the file to the specific S3 bucket
            s3Client.putObject(new PutObjectRequest("bucket", filename, file.getInputStream(), metadata));
            // Return the file URL from S3 bucket
            return s3Client.getUrl("bucket", filename).toString();
        } catch (AmazonServiceException e) {
            throw new RuntimeException("Error uploading file to S3: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            throw new RuntimeException("Error communicating with S3: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file for upload: " + e.getMessage(), e);
        }

    }

}
