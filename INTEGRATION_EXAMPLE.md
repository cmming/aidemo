# MCP与LLM集成示例

## 前提条件

启动应用之前，请确保：
1. Ollama服务正在运行（默认端口11434）
2. 已安装配置的模型（如qwen3:8b）

```bash
# 检查Ollama状态
curl http://localhost:11434/api/tags

# 如果未安装模型，请先安装
ollama pull qwen3:8b
```

## 启动应用

```bash
./mvnw spring-boot:run
```

应用将在 `http://localhost:8080` 启动

## 测试MCP工具集成

### 1. 验证MCP服务器工作正常

```bash
# 健康检查
curl http://localhost:8080/mcp/health

# 查看服务器信息
curl http://localhost:8080/mcp/info

# 列出可用工具
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

### 2. 测试MCP工具（不通过LLM）

```bash
# 直接调用计算器工具
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "calculator",
      "arguments": {
        "operation": "multiply",
        "a": 42,
        "b": 17
      }
    },
    "id": 1
  }'

# 直接调用时间工具
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_current_time",
      "arguments": {
        "timezone": "UTC"
      }
    },
    "id": 2
  }'
```

### 3. 查看MCP聊天可用工具

```bash
curl http://localhost:8080/api/mcp/chat/tools
```

**预期输出**:
```json
[
  "calculate - 执行基本算术运算(add, subtract, multiply, divide)",
  "getCurrentTime - 获取当前日期和时间"
]
```

### 4. 使用LLM调用MCP工具 - 计算器

```bash
# 同步请求
curl "http://localhost:8080/api/mcp/chat/sync?message=用计算器帮我算25加37"

# 流式请求
curl "http://localhost:8080/api/mcp/chat/stream?message=用计算器算42乘以17"
```

**预期流程**:
1. LLM收到消息并识别需要计算
2. LLM调用`calculate`函数，参数为operation="multiply", a=42, b=17
3. MCPCalculatorTool通过MCPService执行计算
4. 返回结果: "Result: 714.00"
5. LLM将结果融入自然语言回复

### 5. 使用LLM调用MCP工具 - 时间

```bash
curl "http://localhost:8080/api/mcp/chat/sync?message=现在几点了？"
```

**预期流程**:
1. LLM识别需要获取时间
2. LLM调用`getCurrentTime`函数
3. MCPTimeTool通过MCPService获取当前时间
4. LLM将时间信息整合到回复中

### 6. 复合任务测试

```bash
curl "http://localhost:8080/api/mcp/chat/test-tools?toolType=both"
```

这会发送预设消息："请先告诉我现在几点，然后用计算器计算 100 除以 4"

**预期流程**:
1. LLM首先调用`getCurrentTime`获取当前时间
2. 然后调用`calculate`执行除法运算
3. LLM整合两个结果生成完整回复

### 7. 流式响应示例

使用浏览器访问或curl查看流式输出：

```bash
curl -N "http://localhost:8080/api/mcp/chat/stream?message=用计算器算123乘以456"
```

流式响应会实时显示LLM的输出，包括工具调用和最终答案。

## 日志验证

在应用日志中，你应该看到类似的输出：

```
INFO ... MCPChatController : Stream MCP chat request from user: mcp-user, message: 用计算器算42乘以17
INFO ... MCPCalculatorTool : MCP Calculator Tool called: operation=multiply, a=42.0, b=17.0
INFO ... MCPService : Handling MCP request: method=tools/call, id=...
INFO ... MCPCalculatorTool : MCP Calculator result: Result: 714.00
```

这确认了完整的工具调用链路：
1. 用户请求到达MCPChatController
2. LLM决定调用MCPCalculatorTool
3. MCPCalculatorTool调用底层MCPService
4. 结果返回给LLM
5. LLM生成最终回复

## 验证清单

- [ ] MCP服务器健康检查通过
- [ ] 可以列出MCP工具
- [ ] 可以直接调用MCP工具（不通过LLM）
- [ ] LLM能识别并调用计算器工具
- [ ] LLM能识别并调用时间工具
- [ ] LLM能正确处理复合任务
- [ ] 日志显示完整的工具调用链路
- [ ] 流式响应正常工作

## API文档

访问Swagger UI查看完整API文档：
```
http://localhost:8080/doc.html
```

在文档中，你可以：
1. 查看所有MCP聊天端点
2. 直接在浏览器中测试API
3. 查看请求/响应示例

## 故障排除

### Ollama连接失败

**症状**: 应用启动时出现Ollama连接错误

**解决**:
```bash
# 检查Ollama是否运行
curl http://localhost:11434/api/tags

# 启动Ollama (如果未运行)
ollama serve
```

### LLM不调用工具

**症状**: LLM直接回答问题，不调用MCP工具

**原因**: 
- 提示词不够明确
- 模型不支持function calling

**解决**:
- 使用明确的提示词，如"用计算器..."
- 确保使用支持function calling的模型

### 工具调用失败

**症状**: 日志显示工具调用错误

**排查**:
```bash
# 1. 测试MCP服务直接调用
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'

# 2. 运行集成测试
./mvnw test -Dtest=MCPChatIntegrationTest

# 3. 检查日志
tail -f logs/spring.log
```

## 性能注意事项

1. **工具调用延迟**: 每次工具调用会增加响应时间
2. **Token消耗**: 工具调用会消耗额外的token
3. **并发限制**: 根据Ollama配置调整并发请求数

## 下一步

1. 添加更多MCP工具（如文件操作、数据库查询等）
2. 实现工具调用的监控和统计
3. 优化工具描述以提高LLM调用准确率
4. 添加工具调用的缓存机制
