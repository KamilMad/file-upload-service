package pl.kamil.file_upload_service.exceptions;

public class S3StorageException extends RuntimeException{
    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
