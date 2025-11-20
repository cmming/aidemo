package com.example.aidemo.mcp.handler;

import com.example.aidemo.mcp.model.JsonRpcRequest;
import com.example.aidemo.mcp.model.JsonRpcResponse;
import com.example.aidemo.mcp.service.MCPService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for MCP protocol
 * Supports real-time bidirectional communication using WebSocket transport
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MCPWebSocketHandler extends TextWebSocketHandler {

    private final MCPService mcpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        super.afterConnectionEstablished(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received WebSocket message: {}", payload);

        try {
            // Parse JSON-RPC request
            JsonRpcRequest request = objectMapper.readValue(payload, JsonRpcRequest.class);
            
            // Handle the request
            JsonRpcResponse response = mcpService.handleRequest(request);
            
            // Send response back
            String responseJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(responseJson));
            
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            // Send error response
            JsonRpcResponse errorResponse = JsonRpcResponse.error(null, -32700, "Parse error: " + e.getMessage());
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(errorJson));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session: {}", session.getId(), exception);
        super.handleTransportError(session, exception);
    }
}
