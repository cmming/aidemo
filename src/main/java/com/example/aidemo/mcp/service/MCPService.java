package com.example.aidemo.mcp.service;

import com.example.aidemo.mcp.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * MCP Protocol Service Implementation
 * Handles MCP protocol methods including tools, resources, and prompts
 */
@Slf4j
@Service
public class MCPService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Store registered tools, resources, and prompts
    private final Map<String, MCPTool> tools = new HashMap<>();
    private final Map<String, MCPResource> resources = new HashMap<>();
    private final Map<String, MCPPrompt> prompts = new HashMap<>();

    public MCPService() {
        // Initialize with some example tools and resources
        initializeDefaultTools();
        initializeDefaultResources();
        initializeDefaultPrompts();
    }

    /**
     * Initialize default MCP tools
     */
    private void initializeDefaultTools() {
        // Example: Calculator tool
        MCPTool calculatorTool = MCPTool.builder()
                .name("calculator")
                .description("Perform basic arithmetic operations")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "operation", Map.of(
                                        "type", "string",
                                        "enum", Arrays.asList("add", "subtract", "multiply", "divide"),
                                        "description", "The arithmetic operation to perform"
                                ),
                                "a", Map.of("type", "number", "description", "First operand"),
                                "b", Map.of("type", "number", "description", "Second operand")
                        ),
                        "required", Arrays.asList("operation", "a", "b")
                ))
                .build();
        tools.put("calculator", calculatorTool);

        // Example: Get current time tool
        MCPTool timeTool = MCPTool.builder()
                .name("get_current_time")
                .description("Get the current date and time")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "timezone", Map.of(
                                        "type", "string",
                                        "description", "Timezone (e.g., UTC, Asia/Shanghai)"
                                )
                        )
                ))
                .build();
        tools.put("get_current_time", timeTool);
    }

    /**
     * Initialize default MCP resources
     */
    private void initializeDefaultResources() {
        MCPResource exampleResource = MCPResource.builder()
                .uri("resource://example/data")
                .name("Example Data")
                .description("An example resource containing sample data")
                .mimeType("application/json")
                .build();
        resources.put("resource://example/data", exampleResource);
    }

    /**
     * Initialize default MCP prompts
     */
    private void initializeDefaultPrompts() {
        MCPPrompt codeReviewPrompt = MCPPrompt.builder()
                .name("code_review")
                .description("Review code and provide feedback")
                .arguments(Arrays.asList(
                        PromptArgument.builder()
                                .name("code")
                                .description("The code to review")
                                .required(true)
                                .build(),
                        PromptArgument.builder()
                                .name("language")
                                .description("Programming language")
                                .required(false)
                                .build()
                ))
                .build();
        prompts.put("code_review", codeReviewPrompt);
    }

    /**
     * Handle MCP protocol requests
     */
    public JsonRpcResponse handleRequest(JsonRpcRequest request) {
        try {
            log.info("Handling MCP request: method={}, id={}", request.getMethod(), request.getId());
            
            Object result = switch (request.getMethod()) {
                case "initialize" -> handleInitialize(request.getParams());
                case "tools/list" -> handleListTools();
                case "tools/call" -> handleCallTool(request.getParams());
                case "resources/list" -> handleListResources();
                case "resources/read" -> handleReadResource(request.getParams());
                case "prompts/list" -> handleListPrompts();
                case "prompts/get" -> handleGetPrompt(request.getParams());
                default -> throw new IllegalArgumentException("Unknown method: " + request.getMethod());
            };
            
            return JsonRpcResponse.success(request.getId(), result);
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return JsonRpcResponse.error(request.getId(), -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handle initialize request
     */
    private Map<String, Object> handleInitialize(Object params) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of(
                "tools", Map.of("listChanged", false),
                "resources", Map.of("subscribe", false, "listChanged", false),
                "prompts", Map.of("listChanged", false)
        ));
        result.put("serverInfo", Map.of(
                "name", "Spring Boot MCP Server",
                "version", "1.0.0"
        ));
        return result;
    }

    /**
     * Handle list tools request
     */
    private Map<String, Object> handleListTools() {
        return Map.of("tools", new ArrayList<>(tools.values()));
    }

    /**
     * Handle call tool request
     */
    private Map<String, Object> handleCallTool(Object params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> paramsMap = objectMapper.convertValue(params, Map.class);
        String toolName = (String) paramsMap.get("name");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");
        
        if (!tools.containsKey(toolName)) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        
        // Execute the tool
        Object toolResult = executeTool(toolName, arguments);
        
        return Map.of(
                "content", List.of(
                        Map.of(
                                "type", "text",
                                "text", String.valueOf(toolResult)
                        )
                )
        );
    }

    /**
     * Execute a specific tool
     */
    private Object executeTool(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "calculator" -> {
                String operation = (String) arguments.get("operation");
                Number a = (Number) arguments.get("a");
                Number b = (Number) arguments.get("b");
                double result = switch (operation) {
                    case "add" -> a.doubleValue() + b.doubleValue();
                    case "subtract" -> a.doubleValue() - b.doubleValue();
                    case "multiply" -> a.doubleValue() * b.doubleValue();
                    case "divide" -> {
                        if (b.doubleValue() == 0) {
                            throw new IllegalArgumentException("Division by zero");
                        }
                        yield a.doubleValue() / b.doubleValue();
                    }
                    default -> throw new IllegalArgumentException("Unknown operation: " + operation);
                };
                yield String.format("Result: %.2f", result);
            }
            case "get_current_time" -> {
                String timezone = arguments != null ? (String) arguments.get("timezone") : "UTC";
                yield "Current time (" + timezone + "): " + new Date();
            }
            default -> "Tool executed: " + toolName;
        };
    }

    /**
     * Handle list resources request
     */
    private Map<String, Object> handleListResources() {
        return Map.of("resources", new ArrayList<>(resources.values()));
    }

    /**
     * Handle read resource request
     */
    private Map<String, Object> handleReadResource(Object params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> paramsMap = objectMapper.convertValue(params, Map.class);
        String uri = (String) paramsMap.get("uri");
        
        if (!resources.containsKey(uri)) {
            throw new IllegalArgumentException("Resource not found: " + uri);
        }
        
        MCPResource resource = resources.get(uri);
        
        return Map.of(
                "contents", List.of(
                        Map.of(
                                "uri", resource.getUri(),
                                "mimeType", resource.getMimeType(),
                                "text", getResourceContent(uri)
                        )
                )
        );
    }

    /**
     * Get resource content by URI
     */
    private String getResourceContent(String uri) {
        // In a real implementation, this would fetch actual resource data
        return switch (uri) {
            case "resource://example/data" -> "{\"message\": \"This is example data\", \"timestamp\": \"" + new Date() + "\"}";
            default -> "{}";
        };
    }

    /**
     * Handle list prompts request
     */
    private Map<String, Object> handleListPrompts() {
        return Map.of("prompts", new ArrayList<>(prompts.values()));
    }

    /**
     * Handle get prompt request
     */
    private Map<String, Object> handleGetPrompt(Object params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> paramsMap = objectMapper.convertValue(params, Map.class);
        String promptName = (String) paramsMap.get("name");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) paramsMap.getOrDefault("arguments", new HashMap<>());
        
        if (!prompts.containsKey(promptName)) {
            throw new IllegalArgumentException("Prompt not found: " + promptName);
        }
        
        MCPPrompt prompt = prompts.get(promptName);
        String promptText = generatePromptText(promptName, arguments);
        
        return Map.of(
                "description", prompt.getDescription(),
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", Map.of(
                                        "type", "text",
                                        "text", promptText
                                )
                        )
                )
        );
    }

    /**
     * Generate prompt text based on prompt name and arguments
     */
    private String generatePromptText(String promptName, Map<String, Object> arguments) {
        return switch (promptName) {
            case "code_review" -> {
                String code = (String) arguments.get("code");
                String language = (String) arguments.getOrDefault("language", "unknown");
                yield String.format("Please review the following %s code:\n\n%s", language, code);
            }
            default -> "Prompt: " + promptName;
        };
    }

    /**
     * Get all registered tools
     * This is used by the adapter to expose tools to the LLM
     */
    public Map<String, MCPTool> getTools() {
        return new HashMap<>(tools);
    }

    /**
     * Get all registered resources
     */
    public Map<String, MCPResource> getResources() {
        return new HashMap<>(resources);
    }

    /**
     * Get all registered prompts
     */
    public Map<String, MCPPrompt> getPrompts() {
        return new HashMap<>(prompts);
    }
}
