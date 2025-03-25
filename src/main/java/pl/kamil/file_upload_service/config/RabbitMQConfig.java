package pl.kamil.file_upload_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue uploadQueue() {
        return new Queue("uploadQueue", true);
    }
}
