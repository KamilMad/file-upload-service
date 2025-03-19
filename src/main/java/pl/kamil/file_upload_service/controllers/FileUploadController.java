package pl.kamil.file_upload_service.controllers;

import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.kamil.file_upload_service.services.FileUploadService;

import java.io.InputStream;

@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileUploadService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.OK).body(fileUrl);
    }


    @GetMapping("/{fileKey}")
    public ResponseEntity<InputStreamResource> getFileById(@PathVariable String fileKey) {
            S3Object s3Object = fileUploadService.getFile(fileKey);

            if (s3Object == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            InputStreamResource resource = new InputStreamResource(s3Object.getObjectContent());

            // Return the byte array as the response body
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header("Content-type", s3Object.getObjectMetadata().getContentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileKey + "\"")
                    .body(resource);
    }
}
