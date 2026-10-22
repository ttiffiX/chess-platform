package chess.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Cấu hình các prefix cho broker đẩy tin nhắn xuống client
        registry.enableSimpleBroker("/topic", "/queue");
        // Cấu hình prefix cho client gửi message lên server
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix cho tin nhắn gửi riêng đến từng user (nếu dùng convertAndSendToUser)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để Client thực hiện Handshake kết nối WebSocket ban đầu
        registry.addEndpoint("/ws-chess")
                .setAllowedOriginPatterns("*") // Hoặc cấu hình domain cụ thể của FE
                .withSockJS();
    }
}