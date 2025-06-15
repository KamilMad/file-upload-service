package pl.kamil.file_upload_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.kamil.file_upload_service.dtos.ApiResponse;
import pl.kamil.file_upload_service.dtos.FileMetadataResponse;
import pl.kamil.file_upload_service.dtos.S3FileResponse;
import pl.kamil.file_upload_service.services.FileUploadService;


@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    // This endpoint only accepts requests with Content-Type: multipart/form-data — like HTML forms or file uploads
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file){

        FileMetadataResponse result = fileUploadService.uploadFile(file);
        System.out.println(result.s3Key());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{key}")
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String key) {
            S3FileResponse response = fileUploadService.loadFileContent(key);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, response.contentType())
                    .body(response.inputStreamResource());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse> deleteFile(@PathVariable String key) {
        fileUploadService.deleteByKey(key);

        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse("File deleted successfully"));
    }

}
