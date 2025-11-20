package com.example.aidemo;

import com.example.aidemo.mcp.tools.MCPCalculatorTool;
import com.example.aidemo.mcp.tools.MCPTimeTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MCP tools with Spring AI
 * Tests that MCP tools can be called directly and work correctly
 */
@SpringBootTest
public class MCPChatIntegrationTest {

    @Autowired
    private MCPCalculatorTool mcpCalculatorTool;

    @Autowired
    private MCPTimeTool mcpTimeTool;

    @Test
    public void testMCPCalculatorToolAdd() {
        String result = mcpCalculatorTool.calculate("add", 10.0, 5.0);
        
        assertNotNull(result);
        assertTrue(result.contains("15"), "Result should contain 15: " + result);
    }

    @Test
    public void testMCPCalculatorToolMultiply() {
        String result = mcpCalculatorTool.calculate("multiply", 7.0, 6.0);
        
        assertNotNull(result);
        assertTrue(result.contains("42"), "Result should contain 42: " + result);
    }

    @Test
    public void testMCPCalculatorToolSubtract() {
        String result = mcpCalculatorTool.calculate("subtract", 20.0, 8.0);
        
        assertNotNull(result);
        assertTrue(result.contains("12"), "Result should contain 12: " + result);
    }

    @Test
    public void testMCPCalculatorToolDivide() {
        String result = mcpCalculatorTool.calculate("divide", 100.0, 4.0);
        
        assertNotNull(result);
        assertTrue(result.contains("25"), "Result should contain 25: " + result);
    }

    @Test
    public void testMCPCalculatorToolDivideByZero() {
        String result = mcpCalculatorTool.calculate("divide", 10.0, 0.0);
        
        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("error") || result.contains("zero"), 
                   "Result should indicate error: " + result);
    }

    @Test
    public void testMCPTimeToolWithUTC() {
        String result = mcpTimeTool.getCurrentTime("UTC");
        
        assertNotNull(result);
        assertTrue(result.contains("UTC") || result.contains("time"), 
                   "Result should contain time information: " + result);
    }

    @Test
    public void testMCPTimeToolWithTimezone() {
        String result = mcpTimeTool.getCurrentTime("Asia/Shanghai");
        
        assertNotNull(result);
        assertFalse(result.contains("错误"), "Should not contain error: " + result);
    }

    @Test
    public void testMCPTimeToolWithoutTimezone() {
        String result = mcpTimeTool.getCurrentTime(null);
        
        assertNotNull(result);
        // Should still work with default timezone
        assertFalse(result.contains("错误"), "Should not contain error: " + result);
    }
}
