package com.example.aidemo.mcp.tools;

import com.example.aidemo.mcp.model.JsonRpcRequest;
import com.example.aidemo.mcp.model.JsonRpcResponse;
import com.example.aidemo.mcp.service.MCPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI Tool that wraps MCP Time tool
 * This allows the LLM to get current time via MCP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MCPTimeTool {

    private final MCPService mcpService;

    /**
     * Get current date and time
     * @param timezone Optional timezone (e.g., "UTC", "Asia/Shanghai")
     * @return Current time information
     */
    @Tool(description = "获取当前日期和时间，可以指定时区")
    public String getCurrentTime(String timezone) {
        log.info("MCP Time Tool called with timezone: {}", timezone);
        
        try {
            // Prepare MCP tool call
            Map<String, Object> arguments = new HashMap<>();
            if (timezone != null && !timezone.isBlank()) {
                arguments.put("timezone", timezone);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("name", "get_current_time");
            params.put("arguments", arguments);

            // Create JSON-RPC request
            JsonRpcRequest request = new JsonRpcRequest();
            request.setMethod("tools/call");
            request.setParams(params);
            request.setId(System.currentTimeMillis());

            // Execute via MCP service
            JsonRpcResponse response = mcpService.handleRequest(request);

            if (response.getError() != null) {
                log.error("MCP time tool error: {}", response.getError().getMessage());
                return "获取时间错误: " + response.getError().getMessage();
            }

            // Extract result
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.getResult();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");

            if (content != null && !content.isEmpty()) {
                Object text = content.get(0).get("text");
                String resultText = text != null ? text.toString() : "已获取时间";
                log.info("MCP Time result: {}", resultText);
                return resultText;
            }

            return "已获取时间但无结果";

        } catch (Exception e) {
            log.error("Error calling MCP time tool", e);
            return "获取时间时出错: " + e.getMessage();
        }
    }
}
