package pl.kamil.file_upload_service.exceptions;

public class FileProcessingException extends RuntimeException{
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
