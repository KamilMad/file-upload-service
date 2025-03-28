package pl.kamil.file_upload_service.dtos;

public record FileMetadataResponse(
        Long id,
        String originalName,
        String contentType,
        long size,
        String url
) { }
