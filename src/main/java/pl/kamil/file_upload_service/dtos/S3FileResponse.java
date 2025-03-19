package pl.kamil.file_upload_service.dtos;

import org.springframework.core.io.InputStreamResource;


public record S3FileResponse(
        InputStreamResource inputStreamResource,
        String contentType) {

}
