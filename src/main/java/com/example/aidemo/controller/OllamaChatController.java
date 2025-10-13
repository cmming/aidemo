package com.example.aidemo.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Tag(name = " Ollama chat client 问答客户端")
@RestController
public class OllamaChatController {

    @Autowired
    private OllamaChatModel chatModel;

    private final AtomicLong messageIdCounter = new AtomicLong(0);
    private static final String CURRENT_USER = "cmming";
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");





    /**
     * 基础流式响应 - 原始代码功能
     */
    @GetMapping(value = "/generateStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStream(
            @RequestParam(value = "message", defaultValue = "你是谁？") String message) {

        // 添加用户上下文信息
        String enhancedMessage = String.format(
                "Current User: cmming\nCurrent Time (UTC): %s\nUser Message: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                message
        );

        Prompt prompt = new Prompt(new UserMessage(enhancedMessage));
        return this.chatModel.stream(prompt);
    }

    /**
     * 高级流式响应 - 带配置选项
     */
    @GetMapping(value = "/generateStreamAdvanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStreamAdvanced(
            @RequestParam(value = "message", defaultValue = "你是谁？") String message,
            @RequestParam(value = "temperature", defaultValue = "0.7") Double temperature,
            @RequestParam(value = "maxTokens", defaultValue = "1024") Integer maxTokens) {

        // 创建自定义配置
        OllamaOptions options = OllamaOptions.builder()
                .model("qwen:1.8b")
                .temperature(temperature)
                .numPredict(maxTokens)
                .keepAlive("5m")
                .build();

        String enhancedMessage = String.format(
                "User: cmming (Time: %s)\nRequest: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                message
        );

        Prompt prompt = new Prompt(new UserMessage(enhancedMessage), options);
        return this.chatModel.stream(prompt);
    }

    /**
     * 流式响应转换为Server-Sent Events (SSE)
     */
    @GetMapping(value = "/generateStreamSSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamSSE(
            @RequestParam(value = "message", defaultValue = "你是谁？") String message) {

        String enhancedMessage = String.format(
                "User: cmming\nTime: %s\nMessage: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                message
        );

        Prompt prompt = new Prompt(new UserMessage(enhancedMessage));

        return this.chatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText()); // 结束标记
    }

    /**
     * 流式响应 - JSON格式输出
     */
    @GetMapping( value = "/generateStreamJson", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> generateStreamJson() {
        Map<String, String> request = new HashMap<>();

        String message = request.getOrDefault("message", "你是谁？");

        String enhancedMessage = String.format(
                "User: cmming\nCurrent Time (UTC): %s\nUser Input: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                message
        );

        Prompt prompt = new Prompt(new UserMessage(enhancedMessage));

        return this.chatModel.stream(prompt)
                .map(response -> {
                    return Map.of(
                            "user", "cmming",
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            "content", Objects.requireNonNull(response.getResult().getOutput().getText()),
                            "finishReason", response.getResult().getMetadata().getFinishReason() != null ?
                                    response.getResult().getMetadata().getFinishReason() : "continuing",
                            "model", response.getMetadata().getModel()
                    );
                });
    }

    /**
     * 获取当前UTC时间
     */
    private String getCurrentUtcTime() {
        return LocalDateTime.now(ZoneOffset.UTC).format(UTC_FORMATTER);
    }

    /**
     * 基础SSE流式响应 - 符合SSE规范
     */
    @GetMapping(value = "/basic", produces = "text/event-stream")
    public Flux<String> basicSSEStream(
            @RequestParam(value = "message", defaultValue = "你是谁？") String message) {

        String contextualMessage = String.format("""
            Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted): %s
            Current User's Login: %s
            
            User Message: %s
            
            Please provide a helpful response.
            """,
                getCurrentUtcTime(),
                CURRENT_USER,
                message
        );

        Prompt prompt = new Prompt(new UserMessage(contextualMessage));

        return this.chatModel.stream(prompt)
                .mapNotNull(response -> {
                    return response.getResult().getOutput().getText();
                }); // 流结束标记
    }
}
