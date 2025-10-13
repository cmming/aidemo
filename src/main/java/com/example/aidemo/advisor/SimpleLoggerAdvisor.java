package com.example.aidemo.advisor;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;

@Slf4j
@Component
public class SimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {


    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 1; // 执行顺序
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        logRequest(chatClientRequest);

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        logResponse(chatClientResponse);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        logRequest(chatClientRequest);

        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }


    private void logRequest(ChatClientRequest request) {
        log.info("request: {}", request);
    }

    private void logResponse(ChatClientResponse chatClientResponse) {
        log.info("response: {}", chatClientResponse);
        String aaa = chatClientResponse.chatResponse().getResults().get(0).getOutput().getText() + "cm";
    }




    private boolean containsSensitiveContent(String content) {
        // 简单的敏感词检测
        String[] sensitiveWords = {"暴力", "违法", "危险"};
        return Arrays.stream(sensitiveWords)
                .anyMatch(word -> content.toLowerCase().contains(word));
    }

    private String filterSensitiveContent(String content) {
        // 简单的内容过滤
        return content.replaceAll("(?i)(暴力|违法|危险)", "***");
    }
}
