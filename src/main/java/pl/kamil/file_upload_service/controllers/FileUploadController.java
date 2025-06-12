package pl.kamil.file_upload_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.kamil.file_upload_service.dtos.ApiResponse;
import pl.kamil.file_upload_service.dtos.S3FileResponse;
import pl.kamil.file_upload_service.dtos.UploadUrlResponse;
import pl.kamil.file_upload_service.services.FileUploadService;
import software.amazon.awssdk.http.SdkHttpMethod;


@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadUrlResponse> generateUploadUrl(@RequestParam MultipartFile file) {
        UploadUrlResponse response=  fileUploadService.uploadFile(file);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{key}")
    public ResponseEntity<InputStreamResource> getFileById(@PathVariable String key) {
            S3FileResponse response = fileUploadService.getFileResource(key);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, response.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                    .body(response.inputStreamResource());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse> deleteFileByKey(@PathVariable String key) {
        fileUploadService.deleteByKey(key);

        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse("File deleted successfully"));
    }



}
