# MCP (Model Context Protocol) Implementation

本项目已实现基于Spring Boot的MCP（模型上下文协议）服务器。

## 功能特性

### 1. JSON-RPC 2.0 协议支持
- 完整实现JSON-RPC 2.0规范
- 支持单个请求和批量请求
- 标准错误处理机制

### 2. 双通道通信
- **HTTP REST API**: `/mcp` 端点用于标准HTTP请求
- **WebSocket**: `/mcp/ws` 端点用于实时双向通信

### 3. MCP核心功能

#### 工具(Tools)
实现了可调用的工具功能：
- `calculator`: 基础算术运算 (加、减、乘、除)
- `get_current_time`: 获取当前时间

支持的方法:
- `tools/list`: 列出所有可用工具
- `tools/call`: 调用特定工具

#### 资源(Resources)
提供上下文数据资源:
- 资源URI形式: `resource://example/data`
- 支持不同MIME类型

支持的方法:
- `resources/list`: 列出所有可用资源
- `resources/read`: 读取特定资源内容

#### 提示词(Prompts)
预定义的提示词模板:
- `code_review`: 代码审查提示词模板

支持的方法:
- `prompts/list`: 列出所有提示词
- `prompts/get`: 获取特定提示词

### 4. 协议初始化
- `initialize`: 协议握手和能力交换

## API端点

### HTTP REST API

#### 1. MCP JSON-RPC端点
```
POST /mcp
Content-Type: application/json
```

请求示例（初始化）:
```json
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {}
  },
  "id": 1
}
```

响应:
```json
{
  "jsonrpc": "2.0",
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {"listChanged": false},
      "resources": {"subscribe": false, "listChanged": false},
      "prompts": {"listChanged": false}
    },
    "serverInfo": {
      "name": "Spring Boot MCP Server",
      "version": "1.0.0"
    }
  },
  "id": 1
}
```

#### 2. 列出工具
```json
{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "id": 2
}
```

#### 3. 调用工具
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "calculator",
    "arguments": {
      "operation": "add",
      "a": 10,
      "b": 5
    }
  },
  "id": 3
}
```

#### 4. 批量请求
```
POST /mcp/batch
Content-Type: application/json
```

```json
[
  {
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": 1
  },
  {
    "jsonrpc": "2.0",
    "method": "prompts/list",
    "id": 2
  }
]
```

#### 5. 健康检查
```
GET /mcp/health
```

#### 6. 服务器信息
```
GET /mcp/info
```

### WebSocket API

连接到: `ws://localhost:8080/mcp/ws`

发送和接收相同格式的JSON-RPC消息。

## 使用示例

### 使用curl测试

```bash
# 初始化
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "initialize",
    "params": {},
    "id": 1
  }'

# 列出工具
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": 2
  }'

# 使用计算器工具
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "calculator",
      "arguments": {
        "operation": "multiply",
        "a": 7,
        "b": 6
      }
    },
    "id": 3
  }'

# 列出资源
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "resources/list",
    "id": 4
  }'

# 读取资源
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "resources/read",
    "params": {
      "uri": "resource://example/data"
    },
    "id": 5
  }'

# 列出提示词
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "prompts/list",
    "id": 6
  }'

# 获取提示词
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "prompts/get",
    "params": {
      "name": "code_review",
      "arguments": {
        "code": "function add(a, b) { return a + b; }",
        "language": "javascript"
      }
    },
    "id": 7
  }'

# 健康检查
curl http://localhost:8080/mcp/health

# 服务器信息
curl http://localhost:8080/mcp/info
```

### 使用JavaScript (WebSocket)

```javascript
const ws = new WebSocket('ws://localhost:8080/mcp/ws');

ws.onopen = () => {
  console.log('Connected to MCP server');
  
  // 发送初始化请求
  ws.send(JSON.stringify({
    jsonrpc: '2.0',
    method: 'initialize',
    params: {},
    id: 1
  }));
};

ws.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('Received:', response);
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};
```

## 架构说明

### 核心组件

1. **模型层 (model)**
   - `JsonRpcRequest`: JSON-RPC请求模型
   - `JsonRpcResponse`: JSON-RPC响应模型
   - `JsonRpcError`: 错误模型
   - `MCPTool`, `MCPResource`, `MCPPrompt`: MCP实体模型

2. **服务层 (service)**
   - `MCPService`: 核心MCP协议处理逻辑

3. **控制器层 (controller)**
   - `MCPController`: HTTP REST API控制器

4. **处理器层 (handler)**
   - `MCPWebSocketHandler`: WebSocket消息处理

5. **配置层 (config)**
   - `MCPWebSocketConfig`: WebSocket配置

## 扩展开发

### 添加新工具

在 `MCPService.initializeDefaultTools()` 中添加:

```java
MCPTool myTool = MCPTool.builder()
    .name("my_tool")
    .description("My custom tool")
    .inputSchema(Map.of(
        "type", "object",
        "properties", Map.of(
            "param1", Map.of("type", "string", "description", "Parameter 1")
        ),
        "required", Arrays.asList("param1")
    ))
    .build();
tools.put("my_tool", myTool);
```

并在 `executeTool()` 方法中实现工具逻辑。

### 添加新资源

在 `MCPService.initializeDefaultResources()` 中添加资源定义，并在 `getResourceContent()` 中实现内容获取逻辑。

### 添加新提示词

在 `MCPService.initializeDefaultPrompts()` 中添加提示词定义，并在 `generatePromptText()` 中实现提示词生成逻辑。

## 依赖项

- Spring Boot 3.5.4
- Spring Boot Starter Web
- Spring Boot Starter WebSocket
- Spring Boot Starter Validation
- Lombok
- Jackson (JSON处理)

## 运行应用

```bash
./mvnw spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

## Swagger文档

访问 `http://localhost:8080/doc.html` 查看完整的API文档。

## 参考资料

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
