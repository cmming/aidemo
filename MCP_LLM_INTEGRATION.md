# MCP与LLM集成文档

## 概述

本项目实现了MCP (Model Context Protocol) 服务与大语言模型 (LLM) 的集成。现在LLM可以在对话过程中主动调用MCP工具来完成特定任务。

## 架构说明

### 集成架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户请求                              │
└────────────────────┬────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│              MCPChatController                           │
│         (/api/mcp/chat/*)                               │
└────────────────────┬────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│          Spring AI ChatClient                            │
│      (配置了MCP工具)                                      │
└───────────┬─────────────────────────┬───────────────────┘
            ↓                         ↓
   ┌────────────────┐        ┌────────────────┐
   │ MCPCalculator  │        │   MCPTimeTool  │
   │     Tool       │        │                │
   └────────┬───────┘        └────────┬───────┘
            ↓                         ↓
   ┌────────────────────────────────────────┐
   │         MCPService                      │
   │    (JSON-RPC工具执行)                    │
   └────────────────────────────────────────┘
```

### 组件说明

1. **MCPChatController** (`/src/main/java/com/example/aidemo/mcp/controller/MCPChatController.java`)
   - 提供支持MCP工具的聊天API端点
   - 配置ChatClient使用MCP工具
   - 管理对话历史和会话

2. **MCP工具包装类**
   - `MCPCalculatorTool`: 包装MCP计算器工具，支持加减乘除运算
   - `MCPTimeTool`: 包装MCP时间工具，获取当前时间

3. **MCPService**
   - 底层MCP协议实现
   - 通过JSON-RPC执行工具调用

## 可用端点

### 1. 同步聊天（支持MCP工具）

```
GET /api/mcp/chat/sync
```

**参数**:
- `message` (required): 用户消息
- `historySize` (optional): 历史记录条数，默认10
- `userId` (optional): 用户ID，默认"mcp-user"

**示例**:
```bash
curl "http://localhost:8080/api/mcp/chat/sync?message=用计算器帮我算15乘以8"
```

### 2. 流式聊天（支持MCP工具）

```
GET /api/mcp/chat/stream
```

**参数**: 与同步聊天相同

**示例**:
```bash
curl "http://localhost:8080/api/mcp/chat/stream?message=现在几点了"
```

### 3. 测试MCP工具

```
GET /api/mcp/chat/test-tools
```

**参数**:
- `toolType` (optional): 测试类型 - "calculator", "time", "both"
- `userId` (optional): 用户ID

**示例**:
```bash
# 测试计算器
curl "http://localhost:8080/api/mcp/chat/test-tools?toolType=calculator"

# 测试时间工具
curl "http://localhost:8080/api/mcp/chat/test-tools?toolType=time"

# 测试所有工具
curl "http://localhost:8080/api/mcp/chat/test-tools?toolType=both"
```

### 4. 查看可用工具

```
GET /api/mcp/chat/tools
```

**示例**:
```bash
curl "http://localhost:8080/api/mcp/chat/tools"
```

**响应**:
```json
[
  "calculate - 执行基本算术运算(add, subtract, multiply, divide)",
  "getCurrentTime - 获取当前日期和时间"
]
```

### 5. 清理聊天记忆

```
POST /api/mcp/chat/memory/clear
```

**参数**:
- `userId` (optional): 要清理的用户ID

**示例**:
```bash
curl -X POST "http://localhost:8080/api/mcp/chat/memory/clear?userId=mcp-user"
```

## 使用示例

### 示例1: 使用计算器

**请求**:
```bash
curl "http://localhost:8080/api/mcp/chat/sync?message=请用计算器帮我计算42乘以17"
```

**工作流程**:
1. LLM接收用户消息
2. LLM识别需要计算，调用`calculate`工具
3. 工具参数: `operation="multiply", a=42, b=17`
4. MCPService执行计算
5. 返回结果: "Result: 714.00"
6. LLM将结果整合到自然语言回复中

### 示例2: 获取时间

**请求**:
```bash
curl "http://localhost:8080/api/mcp/chat/sync?message=现在几点了？"
```

**工作流程**:
1. LLM接收用户消息
2. LLM识别需要获取时间，调用`getCurrentTime`工具
3. MCPService获取当前时间
4. 返回当前时间信息
5. LLM将时间整合到自然语言回复中

### 示例3: 复合任务

**请求**:
```bash
curl "http://localhost:8080/api/mcp/chat/sync?message=请先告诉我现在几点，然后用计算器算100除以4"
```

**工作流程**:
1. LLM分解任务为两个工具调用
2. 第一步: 调用`getCurrentTime`获取时间
3. 第二步: 调用`calculate`执行除法运算
4. LLM整合两个结果并生成完整回复

## 可用的MCP工具

### 1. Calculator (计算器)

**功能**: 执行基本算术运算

**参数**:
- `operation`: 运算类型 ("add", "subtract", "multiply", "divide")
- `a`: 第一个数字
- `b`: 第二个数字

**Spring AI方法名**: `calculate`

**示例触发短语**:
- "用计算器算一下..."
- "帮我计算..."
- "X加Y等于多少"
- "X乘以Y"

### 2. Time Tool (时间工具)

**功能**: 获取当前日期和时间

**参数**:
- `timezone`: 时区 (可选，如 "UTC", "Asia/Shanghai")

**Spring AI方法名**: `getCurrentTime`

**示例触发短语**:
- "现在几点了"
- "告诉我当前时间"
- "今天是几号"

## 技术实现

### 工具包装机制

MCP工具通过Spring AI的`@Tool`注解包装：

```java
@Tool(description = "执行基本算术运算...")
public String calculate(String operation, double a, double b) {
    // 1. 构建MCP JSON-RPC请求
    // 2. 调用MCPService
    // 3. 返回结果
}
```

### 工具注册

在`MCPChatController`中注册工具：

```java
this.chatClient = chatClientBuilder
    .defaultAdvisors(...)
    .build();

// 在请求时添加工具
chatClient.prompt()
    .user(userInput)
    .tools(mcpCalculatorTool, mcpTimeTool)
    .stream()
    .content();
```

## 扩展指南

### 添加新的MCP工具

1. 在MCPService中定义MCP工具（如果还没有）
2. 创建新的工具包装类：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MCPNewTool {
    
    private final MCPService mcpService;
    
    @Tool(description = "工具描述")
    public String methodName(String param1, int param2) {
        // 构建MCP请求
        // 调用mcpService.handleRequest()
        // 返回结果
    }
}
```

3. 在`MCPChatController`中注入并注册新工具：

```java
private final MCPNewTool mcpNewTool;

// 在prompt中添加
.tools(mcpCalculatorTool, mcpTimeTool, mcpNewTool)
```

## 测试

### 单元测试

运行MCP工具测试：
```bash
./mvnw test -Dtest=MCPChatIntegrationTest
```

### 集成测试

运行完整MCP协议测试：
```bash
./mvnw test -Dtest=MCPIntegrationTest
```

## 与原有聊天端点的对比

### 原有端点 (/api/chat/*)

- **特点**: 基础聊天功能，支持对话历史
- **工具**: 仅支持内置的DateTimeTools
- **用途**: 通用对话场景

### MCP集成端点 (/api/mcp/chat/*)

- **特点**: 集成MCP协议，支持动态工具调用
- **工具**: MCP定义的所有工具（计算器、时间等）
- **用途**: 需要调用外部工具的场景
- **优势**: 
  - 工具定义统一（遵循MCP标准）
  - 易于扩展新工具
  - 可与其他MCP客户端共享工具定义

## 故障排查

### 问题：LLM不调用工具

**可能原因**:
1. 用户消息不够明确
2. LLM模型不支持function calling
3. 工具描述不够清晰

**解决方案**:
1. 使用更明确的提示词，如"用计算器..."
2. 确认使用的LLM支持工具调用
3. 优化工具的description

### 问题：工具调用失败

**排查步骤**:
1. 查看日志中的MCP service调用
2. 确认MCPService正常工作（运行MCPIntegrationTest）
3. 检查工具参数是否正确传递

## 参考资料

- [Model Context Protocol 规范](https://modelcontextprotocol.io/)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [MCP服务器实现文档](./MCP_README.md)
- [MCP快速开始](./MCP_QUICKSTART.md)
