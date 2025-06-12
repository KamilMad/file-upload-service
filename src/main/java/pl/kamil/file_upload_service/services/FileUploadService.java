package pl.kamil.file_upload_service.services;

import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.kamil.file_upload_service.dtos.FileMetadataResponse;
import pl.kamil.file_upload_service.dtos.S3FileResponse;
import pl.kamil.file_upload_service.utilities.ExceptionMapper;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.UUID;

@Service
public class FileUploadService {

    private final S3Client s3Client;

    private final static String bucket = "my-upload-file-bucket-085";
    public FileUploadService(S3Client s3Client) {
        this.s3Client = s3Client;

    }

    public FileMetadataResponse uploadFile(MultipartFile file) {
        // Prevent filename collisions
        String s3key = UUID.randomUUID() + "-" + file.getOriginalFilename();

        try (InputStream inputStream = file.getInputStream()) {
            // Upload the file to the specific S3 bucket
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));

            return new FileMetadataResponse(s3key);

        } catch (Exception e) {
            throw ExceptionMapper.mapS3PutException(e);
        }

    }
    public S3FileResponse loadFileContent(String fileKey) {
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
        } catch (Exception e) {
            throw ExceptionMapper.mapS3DeleteException(e, key);
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
        } catch (Exception e) {
            throw ExceptionMapper.mapS3DeleteException(e, fileKey);
        }

    }

}
