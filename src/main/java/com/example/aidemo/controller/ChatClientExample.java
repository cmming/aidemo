package com.example.aidemo.controller;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
//import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ChatClientExample {

//    @Autowired
//    JdbcChatMemoryRepository chatMemoryRepository;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Autowired
    public ChatClientExample(ChatModel chatModel) {

        // 初始化聊天记忆存储 - 用于保存对话历史
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        // 构建ChatClient，配置默认的顾问器(Advisors)
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        // 聊天记忆顾问器 - 负责管理对话上下文和历史记录
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()

                        // RAG (检索增强生成) 顾问器 - 从向量数据库检索相关信息
//                        QuestionAnswerAdvisor.builder(vectorStore)
//                                .searchSimilarityThreshold(0.7)  // 相似度阈值
//                                .searchResultSize(5)             // 检索结果数量
//                                .userTextAdvise("请基于以下相关信息回答问题：{question_answer_context}")
//                                .build()
                )
                .build();
    }

    /**
     * 处理单次对话请求
     * @param userText 用户输入的文本
     * @param conversationId 对话ID，用于区分不同的对话会话
     * @return AI生成的回复
     */
    public String handleSingleMessage(String userText, String conversationId) {
        try {
            String response = this.chatClient.prompt()
                    // 在运行时设置顾问器参数
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(userText)  // 设置用户消息
                    .call()          // 执行调用
                    .content();      // 获取响应内容

            return response;
        } catch (Exception e) {
            // 错误处理
            System.err.println("处理聊天请求时发生错误: " + e.getMessage());
            return "抱歉，处理您的请求时出现了问题，请稍后重试。";
        }
    }

    /**
     * 处理带有系统提示的对话
     * @param userText 用户输入
     * @param systemPrompt 系统提示
     * @param conversationId 对话ID
     * @return AI回复
     */
    public String handleMessageWithSystemPrompt(String userText, String systemPrompt, String conversationId) {
        return this.chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(systemPrompt)  // 设置系统级提示
                .user(userText)
                .call()
                .content();
    }

    /**
     * 流式处理对话，适用于长文本生成
     * @param userText 用户输入
     * @param conversationId 对话ID
     * @param callback 回调函数，用于处理流式响应
     */
    public void handleStreamingMessage(String userText, String conversationId,
                                       java.util.function.Consumer<String> callback) {
        this.chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(userText)
                .stream()  // 启用流式响应
                .content()
                .subscribe(callback::accept);  // 处理每个流式片段
    }

    /**
     * 获取对话历史
     * @param conversationId 对话ID
     * @return 对话历史列表
     */
    public java.util.List<org.springframework.ai.chat.messages.Message> getConversationHistory(String conversationId) {
        return chatMemory.get(conversationId);  // 获取最近50条消息
    }

    /**
     * 清除指定对话的历史记录
     * @param conversationId 对话ID
     */
    public void clearConversationHistory(String conversationId) {
        chatMemory.clear(conversationId);
    }

    /**
     * 添加文档到向量存储，用于RAG检索
     * @param documents 要添加的文档列表
     */
//    public void addDocumentsToVectorStore(java.util.List<org.springframework.ai.document.Document> documents) {
//        vectorStore.add(documents);
//    }
}