package com.example.aidemo.mcp.config;

import com.example.aidemo.mcp.handler.MCPWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for MCP protocol
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class MCPWebSocketConfig implements WebSocketConfigurer {

    private final MCPWebSocketHandler mcpWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mcpWebSocketHandler, "/mcp/ws")
                .setAllowedOrigins("*"); // Configure CORS as needed
    }
}
