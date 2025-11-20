# MCP Server Quick Start Guide

## What is MCP?

MCP (Model Context Protocol) is a protocol that enables AI applications to communicate with context providers. This implementation provides a complete MCP server using Spring Boot that exposes tools, resources, and prompts to AI clients.

## Quick Start

### 1. Start the Server

```bash
./mvnw spring-boot:run
```

The server will start at `http://localhost:8080`

### 2. Try the Interactive Demo

Open your browser and navigate to:
```
http://localhost:8080/mcp-demo
```

This provides an interactive interface to test all MCP features.

### 3. API Endpoints

#### Health Check
```bash
curl http://localhost:8080/mcp/health
```

#### Server Info
```bash
curl http://localhost:8080/mcp/info
```

#### MCP JSON-RPC Endpoint
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"initialize","params":{},"id":1}'
```

## Available MCP Methods

### Protocol Methods

- `initialize` - Initialize the MCP protocol connection

### Tool Methods

- `tools/list` - List all available tools
- `tools/call` - Call a specific tool

### Resource Methods

- `resources/list` - List all available resources
- `resources/read` - Read a specific resource

### Prompt Methods

- `prompts/list` - List all available prompts
- `prompts/get` - Get a specific prompt with arguments

## Example: Using the Calculator Tool

```bash
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
```

Response:
```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Result: 42.00"
      }
    ]
  },
  "id": 3
}
```

## WebSocket Support

Connect to the WebSocket endpoint for real-time communication:

```javascript
const ws = new WebSocket('ws://localhost:8080/mcp/ws');

ws.onopen = () => {
  ws.send(JSON.stringify({
    jsonrpc: '2.0',
    method: 'tools/list',
    id: 1
  }));
};

ws.onmessage = (event) => {
  console.log('Received:', JSON.parse(event.data));
};
```

## Built-in Tools

### 1. Calculator
Performs basic arithmetic operations (add, subtract, multiply, divide)

**Example:**
```json
{
  "name": "calculator",
  "arguments": {
    "operation": "add",
    "a": 10,
    "b": 5
  }
}
```

### 2. Get Current Time
Returns the current date and time

**Example:**
```json
{
  "name": "get_current_time",
  "arguments": {
    "timezone": "UTC"
  }
}
```

## Built-in Resources

### Example Data Resource
URI: `resource://example/data`

Returns sample JSON data.

## Built-in Prompts

### Code Review Prompt
Name: `code_review`

Generates a prompt for reviewing code.

**Example:**
```json
{
  "name": "code_review",
  "arguments": {
    "code": "function add(a, b) { return a + b; }",
    "language": "javascript"
  }
}
```

## Testing

Run the integration tests:
```bash
./mvnw test -Dtest=MCPIntegrationTest
```

All 8 tests should pass, covering:
- Protocol initialization
- Tools listing and calling
- Resources listing and reading
- Prompts listing and getting
- Error handling

## API Documentation

Full API documentation is available at:
```
http://localhost:8080/doc.html
```

## Architecture

```
┌─────────────────────────────────────────┐
│         MCP Server (Spring Boot)        │
├─────────────────────────────────────────┤
│                                         │
│  ┌────────────┐      ┌──────────────┐  │
│  │  HTTP/REST │      │  WebSocket   │  │
│  │  /mcp      │      │  /mcp/ws     │  │
│  └────────────┘      └──────────────┘  │
│         │                    │          │
│         └──────────┬─────────┘          │
│                    ↓                    │
│         ┌─────────────────────┐         │
│         │    MCPService       │         │
│         │  (JSON-RPC Handler) │         │
│         └─────────────────────┘         │
│                    │                    │
│     ┌──────────────┼──────────────┐     │
│     ↓              ↓              ↓     │
│  ┌──────┐    ┌──────────┐   ┌────────┐ │
│  │Tools │    │Resources │   │Prompts │ │
│  └──────┘    └──────────┘   └────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

## Extending the Server

### Adding a New Tool

1. Edit `MCPService.java`
2. Add tool definition in `initializeDefaultTools()`
3. Implement tool logic in `executeTool()`

### Adding a New Resource

1. Edit `MCPService.java`
2. Add resource definition in `initializeDefaultResources()`
3. Implement resource content in `getResourceContent()`

### Adding a New Prompt

1. Edit `MCPService.java`
2. Add prompt definition in `initializeDefaultPrompts()`
3. Implement prompt generation in `generatePromptText()`

## Troubleshooting

### Server won't start
- Check if port 8080 is available
- Verify Java 17 is installed

### WebSocket connection fails
- Ensure the server is running
- Check browser console for CORS issues
- Verify WebSocket URL is correct

### Tests failing
- Some original tests try to connect to Ollama (not required for MCP)
- Run MCP tests specifically: `./mvnw test -Dtest=MCPIntegrationTest`

## Learn More

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- Full documentation: [MCP_README.md](./MCP_README.md)
