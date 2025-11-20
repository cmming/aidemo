package com.example.aidemo.mcp.tools;

import com.example.aidemo.mcp.model.JsonRpcRequest;
import com.example.aidemo.mcp.model.JsonRpcResponse;
import com.example.aidemo.mcp.service.MCPService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI Tool that wraps MCP Calculator tool
 * This allows the LLM to perform arithmetic operations via MCP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MCPCalculatorTool {

    private final MCPService mcpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Perform arithmetic operation using MCP calculator tool
     * @param operation The operation to perform: add, subtract, multiply, or divide
     * @param a First number
     * @param b Second number
     * @return The result of the calculation
     */
    @Tool(description = "执行基本算术运算。支持加法(add)、减法(subtract)、乘法(multiply)和除法(divide)")
    public String calculate(String operation, double a, double b) {
        log.info("MCP Calculator Tool called: operation={}, a={}, b={}", operation, a, b);
        
        try {
            // Prepare MCP tool call
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("operation", operation);
            arguments.put("a", a);
            arguments.put("b", b);

            Map<String, Object> params = new HashMap<>();
            params.put("name", "calculator");
            params.put("arguments", arguments);

            // Create JSON-RPC request
            JsonRpcRequest request = new JsonRpcRequest();
            request.setMethod("tools/call");
            request.setParams(params);
            request.setId(System.currentTimeMillis());

            // Execute via MCP service
            JsonRpcResponse response = mcpService.handleRequest(request);

            if (response.getError() != null) {
                log.error("MCP calculator error: {}", response.getError().getMessage());
                return "计算错误: " + response.getError().getMessage();
            }

            // Extract result
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.getResult();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");

            if (content != null && !content.isEmpty()) {
                Object text = content.get(0).get("text");
                String resultText = text != null ? text.toString() : "计算完成";
                log.info("MCP Calculator result: {}", resultText);
                return resultText;
            }

            return "计算完成但无结果";

        } catch (Exception e) {
            log.error("Error calling MCP calculator", e);
            return "调用计算器时出错: " + e.getMessage();
        }
    }
}
