package pl.kamil.file_upload_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kamil.file_upload_service.models.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

}
