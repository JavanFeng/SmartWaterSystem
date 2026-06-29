package com.javan.smart.water.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WaterMcpToolConfig {
    private static final Logger logger = LoggerFactory.getLogger(WaterMcpToolConfig.class);

    @Bean
    public ToolCallbackProvider powerMcpTools(@Autowired List<IMcpTool> tools) {
        logger.info("注册MCP工具:{}", tools.stream().map(IMcpTool::getClass).map(Class::toString).collect(Collectors.joining(";")));
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        tools.toArray()
                )
                .build();
    }
}
