package com.example.aidemo.controller;

import com.example.aidemo.advisor.SimpleLoggerAdvisor;
import com.example.aidemo.advisor.ThinkRemovalAdvisor;
import com.example.aidemo.toolCalling.DateTimeTools;
import com.example.aidemo.vo.MockUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * 聊天客户端控制器
 * 提供AI聊天服务的REST API接口，支持同步和流式响应
 *
 * @author cmming
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "chatclient 问答客户端")
public class ChatClientController {

    /**
     * 默认历史记录条数
     */
    private static final int DEFAULT_HISTORY_SIZE = 10;

    /**
     * 最大历史记录条数上限 - 根据成本和上下文窗口调整
     */
    private static final int MAX_HISTORY_SIZE = 50;

    /**
     * 最小历史记录条数下限
     */
    private static final int MIN_HISTORY_SIZE = 1;

    /**
     * AI聊天客户端
     */
    private final ChatClient chatClient;

    /**
     * 聊天记忆存储
     */
    private final ChatMemory chatMemory;

    /**
     * 构造函数 - 初始化聊天客户端和相关顾问
     *
     * @param chatClientBuilder   聊天客户端构建器
     * @param simpleLoggerAdvisor 简单日志顾问
     * @param chatMemory          聊天记忆存储
     */
    public ChatClientController(ChatClient.Builder chatClientBuilder, SimpleLoggerAdvisor simpleLoggerAdvisor, ThinkRemovalAdvisor thinkRemovalAdvisor, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        thinkRemovalAdvisor, // 去掉thinking
                        simpleLoggerAdvisor, // 日志记录顾问
                        new SafeGuardAdvisor(List.of("远光"), "抱歉包含非法内容", 10), // 内容安全过滤顾问
                        MessageChatMemoryAdvisor.builder(chatMemory).build() // 聊天记忆顾问
                )
                .build();
        this.chatMemory = chatMemory;
    }

    /**
     * 同步聊天接口
     * 等待完整响应后一次性返回结果
     *
     * @param userInput   用户输入消息
     * @param historySize 历史记录条数
     * @param userId      用户ID，用于区分不同会话
     * @return 完整的AI响应内容
     */
    @Operation(summary = "大模型同步聊天 - 返回完整文本响应")
    @GetMapping("/sync")
    public String sync(@RequestParam(value = "message", defaultValue = "你是谁？") String userInput,
                       @RequestParam(required = false) Integer historySize,
                       @RequestParam(required = false, defaultValue = "test") String userId) {
        return stream(userInput, historySize, userId)
                .collect(Collectors.joining())
                .block();
    }

    /**
     * 流式聊天接口
     * 使用Server-Sent Events实时返回AI响应内容
     *
     * @param userInput   用户输入消息
     * @param historySize 历史记录条数（可选）
     * @param userId      用户ID，用于区分不同会话
     * @return 流式响应内容
     */
    @Operation(summary = "大模型流式聊天 - SSE实时响应")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(value = "message", defaultValue = "你是谁？") String userInput,
                               @RequestParam(required = false) Integer historySize,
                               @RequestParam(required = false, defaultValue = "test") String userId) {

        // 参数校验和默认值设置
        if (userInput == null || userInput.trim().isEmpty()) {
            return Flux.just("用户输入不能为空");
        }

        // 历史记录条数校验和边界处理
        int size = validateHistorySize(historySize);

        return this.chatClient.prompt()
                .user(userInput)
                .advisors(spec -> spec
                        .param(CONVERSATION_ID, userId) // 设置会话ID
                        .param("chat_memory_response_size", size) // 设置历史记录条数
                )
                .stream()
                .content()
                .map(this::ensureUtf8Encoding); // 确保UTF-8编码
    }

    /**
     * 清理指定用户的聊天记忆
     *
     * @param userId 用户ID
     * @return 清理结果响应
     */
    @Operation(summary = "清理聊天记忆")
    @PostMapping("/memory")
    public ResponseEntity<String> clearMemory(@RequestParam(required = false, defaultValue = "test") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body("userId 不能为空");
        }

        try {
            chatMemory.clear(userId);
            return ResponseEntity.ok("用户 " + userId + " 的聊天记忆已清理");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("清理聊天记忆失败: " + e.getMessage());
        }
    }

    @Operation(summary = "大模型流式聊天 - SSE实时响应-结构化数据 outputBean")
    @GetMapping(value = "/outputBean")
    public MockUserVo outputBean(@RequestParam(value = "message", defaultValue = "你是谁？") String userInput,
                                 @RequestParam(required = false) Integer historySize,
                                 @RequestParam(required = false, defaultValue = "test") String userId) {
//        // 参数校验和默认值设置
//        if (userInput == null || userInput.trim().isEmpty()) {
//            return Flux.just("用户输入不能为空");
//        }

        // 历史记录条数校验和边界处理
        int size = validateHistorySize(historySize);


        return this.chatClient.prompt()
                .system("no_think")
                .user(userInput)
                .advisors(
                        spec -> spec
                                .param(CONVERSATION_ID, userId) // 设置会话ID
                                .param("chat_memory_response_size", size) // 设置历史记录条数
                )
                .call()
                .entity(MockUserVo.class); // 确保UTF-8编码non-thinking
    }

    @Operation(summary = "大模型流式聊天 - SSE实时响应-结构化数据 outputBean")
    @GetMapping(value = "/outputBeans")
    public List<MockUserVo> outputBeans(@RequestParam(value = "message", defaultValue = "你是谁？") String userInput,
                                        @RequestParam(required = false) Integer historySize,
                                        @RequestParam(required = false, defaultValue = "test") String userId) {
//        // 参数校验和默认值设置
//        if (userInput == null || userInput.trim().isEmpty()) {
//            return Flux.just("用户输入不能为空");
//        }

        // 历史记录条数校验和边界处理
        int size = validateHistorySize(historySize);


        return this.chatClient.prompt()
                .system("no_think")
                .user(userInput)
                .advisors(
                        spec -> spec
                                .param(CONVERSATION_ID, userId) // 设置会话ID
                                .param("chat_memory_response_size", size) // 设置历史记录条数
                )
                .call()
//                .entity(MockUserVo.class); // 确保UTF-8编码non-thinking
                .entity(new ParameterizedTypeReference<List<MockUserVo>>() {});
    }


    @Operation(summary = "大模型流式聊天 - toolCalling")
    @GetMapping(value = "/toolCalling")
    public String toolCalling(@RequestParam(value = "message", defaultValue = "你是谁？") String userInput,
                              @RequestParam(required = false) Integer historySize,
                              @RequestParam(required = false, defaultValue = "test") String userId) {
//        // 参数校验和默认值设置
//        if (userInput == null || userInput.trim().isEmpty()) {
//            return Flux.just("用户输入不能为空");
//        }

        // 历史记录条数校验和边界处理
        int size = validateHistorySize(historySize);


        return this.chatClient.prompt()
                .system("no_think")
                .user(userInput)
                .tools(new DateTimeTools())
                .advisors(
                        spec -> spec
                                .param(CONVERSATION_ID, userId) // 设置会话ID
                                .param("chat_memory_response_size", size) // 设置历史记录条数
                )
                .call()
//                .entity(MockUserVo.class); // 确保UTF-8编码non-thinking
                .content();
    }

    /**
     * 校验并规范化历史记录条数
     *
     * @param historySize 原始历史记录条数
     * @return 校验后的历史记录条数
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
     * 确保字符串为UTF-8编码
     *
     * @param content 原始内容
     * @return UTF-8编码的内容
     */
    private String ensureUtf8Encoding(String content) {
        return new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
