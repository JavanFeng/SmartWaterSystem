package com.javan.smart.water;


import org.springframework.ai.mcp.client.webflux.autoconfigure.StreamableHttpWebFluxTransportAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动类
 */
@SpringBootApplication(exclude = {StreamableHttpWebFluxTransportAutoConfiguration.class})
@EnableAsync
public class SmartWaterSystemApplication implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * 启动
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartWaterSystemApplication.class, args);
    }


    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String accessUrl = "http://localhost:" + port + contextPath + "/water-quality.html";
        System.out.println("\n🎉========================================🎉");
        System.out.println("✅ SMART WATER SYSTEM is ready!");
        System.out.println("🚀 Chat with your agent: " + accessUrl);
        System.out.println("📚 Demo experiences loaded");
        System.out.println("🎉========================================🎉\n");
    }
}
