package com.blokus.blokus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.lang.NonNull;

/**
 * WebSocket configuration for real-time game updates
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        // Enable a simple in-memory message broker for sending messages to clients
        // Client will subscribe to /topic/games/{gameId} to receive updates
        registry.enableSimpleBroker("/topic");
        
        // Prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Register the "/ws-blokus" endpoint for clients to connect to
        registry.addEndpoint("/ws-blokus")
                .withSockJS(); // Enable SockJS fallback for browsers that don't support WebSocket
    }
} 