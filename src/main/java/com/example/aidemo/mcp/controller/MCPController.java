package com.example.aidemo.mcp.controller;

import com.example.aidemo.mcp.model.JsonRpcRequest;
import com.example.aidemo.mcp.model.JsonRpcResponse;
import com.example.aidemo.mcp.service.MCPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) REST Controller
 * Provides HTTP endpoints for MCP protocol communication
 */
@Slf4j
@Tag(name = "MCP Server", description = "Model Context Protocol Server APIs")
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class MCPController {

    private final MCPService mcpService;

    /**
     * Main MCP endpoint for JSON-RPC requests
     */
    @Operation(summary = "MCP JSON-RPC Endpoint", description = "Handle MCP protocol JSON-RPC 2.0 requests")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handleMCPRequest(@RequestBody JsonRpcRequest request) {
        log.info("Received MCP request: {}", request.getMethod());
        return mcpService.handleRequest(request);
    }

    /**
     * Batch MCP requests
     */
    @Operation(summary = "Batch MCP Requests", description = "Handle multiple MCP requests in a single call")
    @PostMapping(value = "/batch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<JsonRpcResponse> handleBatchMCPRequest(@RequestBody List<JsonRpcRequest> requests) {
        log.info("Received batch MCP request with {} items", requests.size());
        return requests.stream()
                .map(mcpService::handleRequest)
                .toList();
    }

    /**
     * Health check endpoint
     */
    @Operation(summary = "Health Check", description = "Check if MCP server is running")
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "service", "MCP Server",
                "version", "1.0.0",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Get server info
     */
    @Operation(summary = "Server Info", description = "Get MCP server information and capabilities")
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "name", "Spring Boot MCP Server",
                "version", "1.0.0",
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "tools", true,
                        "resources", true,
                        "prompts", true
                ),
                "description", "MCP server implementation using Spring Boot"
        );
    }
}
