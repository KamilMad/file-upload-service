package pl.kamil.file_upload_service.exceptions;

public class StorageServiceException extends RuntimeException{
    public StorageServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
