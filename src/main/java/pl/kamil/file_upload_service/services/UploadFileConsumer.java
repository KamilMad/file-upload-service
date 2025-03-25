package pl.kamil.file_upload_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import pl.kamil.file_upload_service.dtos.LessonRequest;

import java.util.Map;

@Service
public class UploadFileConsumer {

   // @RabbitListener(queues = "uploadQueue")
    public void handleFileUpload(String messageJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            LessonRequest request = objectMapper.readValue(messageJson, LessonRequest.class);
            System.out.println("Received file upload request for lesson: " + request.title() + " - File: " + request.content());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to convert JSON to LessonRequest");        }

    }

}
