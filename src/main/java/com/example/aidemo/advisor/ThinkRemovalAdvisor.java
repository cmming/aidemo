package com.example.aidemo.advisor;


import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


@Component
public class ThinkRemovalAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Pattern THINK_PATTERN = Pattern.compile("<think>.*?</think>\\s*", Pattern.DOTALL);
    private static final int ORDER = 2;

    @Override
    public String getName() {
        return "ThinkRemovalAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 调用下一个advisor
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        // 处理响应，移除think标签
        ChatResponse chatResponse = response.chatResponse();
        List<Generation> processedGenerations = chatResponse.getResults().stream()
                .map(this::processGeneration)
                .toList();

        // 创建新的ChatResponse
        ChatResponse newChatResponse = new ChatResponse(processedGenerations, chatResponse.getMetadata());


        // 返回处理后的响应
        return new ChatClientResponse(newChatResponse, response.context());
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // 调用下一个advisor获取流
        Flux<ChatClientResponse> responseFlux = streamAdvisorChain.nextStream(chatClientRequest);

        // 用于累积内容的StringBuilder
        StringBuilder contentBuffer = new StringBuilder();

        return responseFlux.map(response -> {
            ChatResponse chatResponse = response.chatResponse();

            if (chatResponse != null && !chatResponse.getResults().isEmpty()) {
                Generation generation = chatResponse.getResults().get(0);
                AssistantMessage message = generation.getOutput();
                String content = message.getText();

                if (content != null) {
                    contentBuffer.append(content);

                    // 检查是否包含完整的think标签
                    String bufferedContent = contentBuffer.toString();
                    if (bufferedContent.contains("</think>")) {
                        // 移除think标签并清空缓冲区
                        String cleanedContent = THINK_PATTERN.matcher(bufferedContent).replaceAll("");
                        contentBuffer.setLength(0);
                        contentBuffer.append(cleanedContent);

                        // 创建清理后的消息
                        AssistantMessage cleanedMessage = new AssistantMessage(cleanedContent, message.getMetadata());
                        Generation cleanedGeneration = new Generation(cleanedMessage, generation.getMetadata());
                        ChatResponse cleanedChatResponse = new ChatResponse(
                                List.of(cleanedGeneration),
                                chatResponse.getMetadata()
                        );

                        return new ChatClientResponse(cleanedChatResponse, response.context());
                    }
                }
            }

            return response;
        });
    }

    private Generation processGeneration(Generation generation) {
        AssistantMessage message = generation.getOutput();
        String originalContent = message.getText();

        if (originalContent == null) {
            return generation;
        }

        // 移除think标签及其内容
        String cleanedContent = THINK_PATTERN.matcher(originalContent).replaceAll("").trim();

        // 创建新的AssistantMessage
        AssistantMessage newMessage = new AssistantMessage(cleanedContent, message.getMetadata());

        return new Generation(newMessage, generation.getMetadata());
    }
}
