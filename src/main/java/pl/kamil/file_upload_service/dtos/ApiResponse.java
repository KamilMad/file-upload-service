package pl.kamil.file_upload_service.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class ApiResponse {

    private String message;
    private Instant timestamp;

    public ApiResponse(String message) {
        this.message = message;
        this.timestamp = Instant.now();
    }
}

