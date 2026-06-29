package com.javan.smart.water.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.NamedClientMcpTransport;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpStreamableHttpClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author FengJ
 * @description 自定义
 */
@Configuration
@EnableConfigurationProperties({McpStreamableHttpClientProperties.class, McpClientCommonProperties.class})
public class CustomStreamMcpConfig {

    private static final String DEFAULT_TOKEN = "default-token";
    @Bean
    public List<NamedClientMcpTransport> streamableHttpWebFluxClientTransports(
            McpStreamableHttpClientProperties streamableProperties,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider) {

        List<NamedClientMcpTransport> streamableHttpTransports = new ArrayList<>();

        var webClientBuilderTemplate = webClientBuilderProvider.getIfAvailable(WebClient::builder);
        var objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);

        for (Map.Entry<String, McpStreamableHttpClientProperties.ConnectionParameters> serverParameters : streamableProperties.getConnections()
                .entrySet()) {
            var webClientBuilder = webClientBuilderTemplate.clone()
                    .baseUrl(serverParameters.getValue().url());

            // custom header 根据name拿取不同token serverParameters.getKey()
            webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + DEFAULT_TOKEN);


            String streamableHttpEndpoint = serverParameters.getValue().endpoint() != null
                    ? serverParameters.getValue().endpoint() : "/mcp";

            var transport = WebClientStreamableHttpTransport.builder(webClientBuilder)
                    .endpoint(streamableHttpEndpoint)
                    .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                    .build();

            streamableHttpTransports.add(new NamedClientMcpTransport(serverParameters.getKey(), transport));
        }

        return streamableHttpTransports;
    }
}
