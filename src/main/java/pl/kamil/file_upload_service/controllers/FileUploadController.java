package pl.kamil.file_upload_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.kamil.file_upload_service.dtos.FileMetadataResponse;
import pl.kamil.file_upload_service.dtos.S3FileResponse;
import pl.kamil.file_upload_service.models.FileMetadata;
import pl.kamil.file_upload_service.services.FileUploadService;

import javax.print.attribute.standard.Media;


@RestController
@RequestMapping("/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

//    @PostMapping
//    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
//        String fileUrl = fileUploadService.uploadFile(file);
//        return ResponseEntity.status(HttpStatus.CREATED).body(fileUrl);
//    }
    // This endpoint only accepts requests with Content-Type: multipart/form-data — like HTML forms or file uploads
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("lessonId") Long lessonId,
            @RequestParam("userId") Long userId) {

        FileMetadataResponse result = fileUploadService.UploadFileWithMetadata(file, lessonId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{key}")
    public ResponseEntity<InputStreamResource> getFileById(@PathVariable String key) {
            S3FileResponse response = fileUploadService.getFileResource(key);
            // Return the byte array as the response body
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, response.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                    .body(response.inputStreamResource());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteFileByKey(@PathVariable String key) {
        fileUploadService.deleteByKey(key);

        return ResponseEntity.status(HttpStatus.OK).body("Deleted file");
    }
}
