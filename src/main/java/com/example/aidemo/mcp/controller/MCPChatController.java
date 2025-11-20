package com.example.aidemo.mcp.controller;

import com.example.aidemo.advisor.SimpleLoggerAdvisor;
import com.example.aidemo.advisor.ThinkRemovalAdvisor;
import com.example.aidemo.mcp.tools.MCPCalculatorTool;
import com.example.aidemo.mcp.tools.MCPTimeTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Chat Controller with MCP Tool Integration
 * This controller enables LLM to use MCP tools during conversations
 */
@Slf4j
@Tag(name = "MCP Chat", description = "Chat with LLM using MCP tools")
@RestController
@RequestMapping("/api/mcp/chat")
public class MCPChatController {

    private static final int DEFAULT_HISTORY_SIZE = 10;
    private static final int MAX_HISTORY_SIZE = 50;
    private static final int MIN_HISTORY_SIZE = 1;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final MCPCalculatorTool mcpCalculatorTool;
    private final MCPTimeTool mcpTimeTool;

    public MCPChatController(
            ChatClient.Builder chatClientBuilder,
            SimpleLoggerAdvisor simpleLoggerAdvisor,
            ThinkRemovalAdvisor thinkRemovalAdvisor,
            ChatMemory chatMemory,
            MCPCalculatorTool mcpCalculatorTool,
            MCPTimeTool mcpTimeTool) {
        
        this.chatMemory = chatMemory;
        this.mcpCalculatorTool = mcpCalculatorTool;
        this.mcpTimeTool = mcpTimeTool;
        
        // Build ChatClient with MCP tools
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        thinkRemovalAdvisor,
                        simpleLoggerAdvisor,
                        new SafeGuardAdvisor(List.of("远光"), "抱歉包含非法内容", 10),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
        
        log.info("MCPChatController initialized with MCP tools");
    }

    /**
     * Synchronous chat with MCP tools enabled
     */
    @Operation(summary = "同步聊天（支持MCP工具）", 
               description = "与AI聊天，AI可以调用MCP工具如计算器、获取时间等")
    @GetMapping("/sync")
    public String syncChat(
            @RequestParam(value = "message", defaultValue = "用计算器计算 15 乘以 8") String userInput,
            @RequestParam(required = false) Integer historySize,
            @RequestParam(required = false, defaultValue = "mcp-user") String userId) {
        
        log.info("Sync MCP chat request from user: {}, message: {}", userId, userInput);
        
        return streamChat(userInput, historySize, userId)
                .collectList()
                .map(list -> String.join("", list))
                .block();
    }

    /**
     * Streaming chat with MCP tools enabled
     */
    @Operation(summary = "流式聊天（支持MCP工具）",
               description = "流式响应，AI可以调用MCP工具。例如：'用计算器算一下 25 加 17'")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam(value = "message", defaultValue = "现在几点了？") String userInput,
            @RequestParam(required = false) Integer historySize,
            @RequestParam(required = false, defaultValue = "mcp-user") String userId) {
        
        log.info("Stream MCP chat request from user: {}, message: {}", userId, userInput);
        
        if (userInput == null || userInput.trim().isEmpty()) {
            return Flux.just("用户输入不能为空");
        }

        int size = validateHistorySize(historySize);

        return chatClient
                .prompt()
                .user(userInput)
                .tools(mcpCalculatorTool, mcpTimeTool)
                .advisors(spec -> spec
                        .param(CONVERSATION_ID, userId)
                        .param("chat_memory_response_size", size)
                )
                .stream()
                .content()
                .map(this::ensureUtf8Encoding);
    }

    /**
     * Call chat with a specific prompt designed to test MCP tools
     */
    @Operation(summary = "测试MCP工具",
               description = "使用预设问题测试MCP工具功能")
    @GetMapping("/test-tools")
    public String testTools(
            @RequestParam(required = false, defaultValue = "calculator") String toolType,
            @RequestParam(required = false, defaultValue = "mcp-test") String userId) {
        
        String message = switch (toolType) {
            case "calculator" -> "请用计算器帮我计算：42 乘以 17，然后告诉我结果";
            case "time" -> "请告诉我现在的时间";
            case "both" -> "请先告诉我现在几点，然后用计算器计算 100 除以 4";
            default -> "请用可用的工具帮我完成一个任务";
        };
        
        log.info("Testing MCP tools with type: {}, message: {}", toolType, message);
        
        return syncChat(message, 5, userId);
    }

    /**
     * Clear chat memory for a user
     */
    @Operation(summary = "清理MCP聊天记忆")
    @PostMapping("/memory/clear")
    public ResponseEntity<String> clearMemory(
            @RequestParam(required = false, defaultValue = "mcp-user") String userId) {
        
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body("userId 不能为空");
        }

        try {
            chatMemory.clear(userId);
            log.info("Cleared chat memory for user: {}", userId);
            return ResponseEntity.ok("用户 " + userId + " 的聊天记忆已清理");
        } catch (Exception e) {
            log.error("Failed to clear memory for user: " + userId, e);
            return ResponseEntity.internalServerError().body("清理聊天记忆失败: " + e.getMessage());
        }
    }

    /**
     * Get available MCP tools
     */
    @Operation(summary = "查看可用的MCP工具")
    @GetMapping("/tools")
    public ResponseEntity<?> getAvailableTools() {
        return ResponseEntity.ok(List.of(
            "calculate - 执行基本算术运算(add, subtract, multiply, divide)",
            "getCurrentTime - 获取当前日期和时间"
        ));
    }

    /**
     * Validate and normalize history size
     */
    private int validateHistorySize(Integer historySize) {
        int size = Objects.requireNonNullElse(historySize, DEFAULT_HISTORY_SIZE);

        if (size < MIN_HISTORY_SIZE) {
            size = DEFAULT_HISTORY_SIZE;
        } else if (size > MAX_HISTORY_SIZE) {
            size = MAX_HISTORY_SIZE;
        }

        return size;
    }

    /**
     * Ensure UTF-8 encoding
     */
    private String ensureUtf8Encoding(String content) {
        return new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
