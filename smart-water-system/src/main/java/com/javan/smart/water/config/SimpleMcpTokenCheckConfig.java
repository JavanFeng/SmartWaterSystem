package com.javan.smart.water.config;

import io.modelcontextprotocol.client.transport.customizer.McpAsyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.reactivestreams.Publisher;
import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * @author FengJ
 * @description 校验mcp
 */
//@Configuration
public class SimpleMcpTokenCheckConfig {
    private static final String DEFAULT_TOKEN = "default-token";

    @Bean
    public McpSyncHttpClientRequestCustomizer syncMcp() {
        return (builder, method, endpoint, body, context) -> {
            // 额外的校验和业务操作,根据url啥的,这里就简单
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + DEFAULT_TOKEN);
        };
    }


    @Bean
    public McpAsyncHttpClientRequestCustomizer asyncMcp() {
        return (builder, method, endpoint, body, context) -> {
            // 额外的校验和业务操作,根据url啥的,这里就简单
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + DEFAULT_TOKEN);
            return Mono.just(builder);
        };
    }


    /**
     * stream
     */

}
