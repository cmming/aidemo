package com.example.aidemo;

import com.example.aidemo.mcp.model.JsonRpcRequest;
import com.example.aidemo.mcp.model.JsonRpcResponse;
import com.example.aidemo.mcp.service.MCPService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MCP functionality
 */
@SpringBootTest
public class MCPIntegrationTest {

    @Autowired
    private MCPService mcpService;

    @Test
    public void testInitialize() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("initialize");
        request.setParams(Map.of());
        request.setId(1);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getResult());
        assertEquals(1, response.getId());
    }

    @Test
    public void testListTools() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("tools/list");
        request.setId(2);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getResult());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertTrue(result.containsKey("tools"));
    }

    @Test
    public void testCallCalculatorTool() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("tools/call");
        request.setParams(Map.of(
                "name", "calculator",
                "arguments", Map.of(
                        "operation", "add",
                        "a", 10,
                        "b", 5
                )
        ));
        request.setId(3);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getResult());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertTrue(result.containsKey("content"));
    }

    @Test
    public void testListResources() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("resources/list");
        request.setId(4);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getResult());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertTrue(result.containsKey("resources"));
    }

    @Test
    public void testReadResource() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("resources/read");
        request.setParams(Map.of("uri", "resource://example/data"));
        request.setId(5);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getResult());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertTrue(result.containsKey("contents"));
    }

    @Test
    public void testListPrompts() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("prompts/list");
        request.setId(6);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getResult());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertTrue(result.containsKey("prompts"));
    }

    @Test
    public void testGetPrompt() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("prompts/get");
        request.setParams(Map.of(
                "name", "code_review",
                "arguments", Map.of(
                        "code", "function test() { return true; }",
                        "language", "javascript"
                )
        ));
        request.setId(7);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNull(response.getError());
        assertNotNull(response.getResult());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertTrue(result.containsKey("messages"));
    }

    @Test
    public void testInvalidMethod() {
        JsonRpcRequest request = new JsonRpcRequest();
        request.setMethod("invalid/method");
        request.setId(8);

        JsonRpcResponse response = mcpService.handleRequest(request);

        assertNotNull(response);
        assertNotNull(response.getError());
        assertEquals(-32603, response.getError().getCode());
    }
}
