package pl.kamil.file_upload_service.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class FileMetadata {

    @Id
    private UUID id;
    private String originalDate;
    private String contentType;
    private long size;
    private String s3Key;
    private long uploadedBy;
    private long lessonId;
}
