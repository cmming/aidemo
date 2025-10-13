package com.example.aidemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AidemoApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private OllamaChatModel chatModel;

    @Test
    void ollamaChat() {
        String systemPrompt = "你是一个专业的客服助手，请用友好和专业的语气回答用户问题。";
        String query = "今天天气怎么样，适合干什么事情？";

        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(query)
        );

        String aaa = chatModel.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getText();
        System.out.println(aaa);
    }

}
