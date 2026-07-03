/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javan.smart.water.agent.router.intent.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.javan.smart.water.agent.router.intent.IRecognition;
import com.javan.smart.water.agent.router.model.IntentResp;
import com.javan.smart.water.agent.router.model.RecognitionResult;
import com.javan.smart.water.agent.router.model.RecognitionStatus;
import com.javan.smart.water.common.constant.AgentConstant;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.enums.IntentEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * LLM base
 *
 * @author Javan
 * @since 1.0.0
 */
@Component
@Order(AgentConstant.INTENT_LLM_ORDER)
public class LLMBasedRecognition implements IRecognition {

    private static final Logger logger = LoggerFactory.getLogger(LLMBasedRecognition.class);
    private static final ExecutorService LLM_EXECUTOR = Executors.newFixedThreadPool(100);
    private final ChatClient chatClient;
    private final static String recognitionId = "llm-base-recognition";
    /**
     * 根据需要修改持久化
     */
    private static final MessageWindowChatMemory CHAT_MEMORY = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(25).build();
    @Value("classpath:/prompts/intent-prompt.md")
    private Resource intentPromptResource;

    @Autowired
    public LLMBasedRecognition(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.clone()
                .build();
    }

    @Override
    public RecognitionResult recognize(OverAllState state, RunnableConfig config) {
        try {
            return recognizeAsync(state, config).get(getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Synchronous wait failed for intent recognition: {}", recognitionId, e);
            RecognitionResult fallback = new RecognitionResult();
            fallback.setRecognitionName(recognitionId);
            fallback.setStatus(RecognitionStatus.ERROR);
            fallback.setErrorMessage(e.getMessage());
            return fallback;
        }
    }

    @Override
    public CompletableFuture<RecognitionResult> recognizeAsync(OverAllState state, RunnableConfig config) {
        return CompletableFuture.supplyAsync(() -> {
                    String userInput = state.value(GraphConstant.INPUT_KEY, "");
                    String conversationId = config.threadId().orElse("default");
                    RecognitionResult result = new RecognitionResult();
                    result.setRecognitionName(recognitionId);
                    result.setStartTimeMillis(System.currentTimeMillis());
                    try {
                        // Build prompt
                        PromptTemplate promptTemplate = new PromptTemplate(intentPromptResource);
                        Prompt prompt = promptTemplate.create(Map.of(
                                "input", userInput,
                                "intent_target", IntentEnum.buildIntentInfo(),
                                "intent_llm_rule", IntentEnum.buildLLMRule()

                        ));
                        String promptText = prompt.getContents();
                        result.setRawPrompt(promptText);
                        logger.debug("LLMBasedRecognition {} with LLM, prompt: {}, chatClient: {} ",
                                recognitionId, promptText.replace("\n", "\\n"),
                                chatClient.getClass().getSimpleName());

                        // Call LLM with optional ChatOptions
                        IntentResp intentResp = chatClient.prompt(prompt)
                                .advisors(SimpleLoggerAdvisor.builder().build(),
                                        MessageChatMemoryAdvisor.builder(CHAT_MEMORY)
                                                .conversationId(conversationId).build())
                                .call().entity(IntentResp.class);
                        IntentEnum intent = null;
                        if (null != intentResp) {
                            intent = IntentEnum.fromIntent(intentResp.intent());
                        }
                        if (null == intentResp || null == intent) {
                            logger.warn("ChatModel returned empty response for {}", recognitionId);
                            result.setStatus(RecognitionStatus.ERROR);
                            result.setErrorMessage("LLM response was empty");
                            return result;
                        }

                        result.setIntent(intent);
                        result.setMetadata(intentResp.getMetadata());
                        // Parse response based on result type and reasoning policy
                        result.setStatus(RecognitionStatus.SUCCESS);

                    } catch (Exception e) {
                        logger.error("Error intent recognize {}: {}",
                                recognitionId, e.getMessage(), e);
                        result.setStatus(RecognitionStatus.ERROR);
                        result.setErrorMessage(e.getMessage());
                    } finally {
                        result.setEndTimeMillis(System.currentTimeMillis());
                    }

                    return result;
                }, LLM_EXECUTOR).orTimeout(getTimeoutMs(), TimeUnit.MILLISECONDS)
                .exceptionally(e -> {
                    RecognitionResult result = new RecognitionResult();
                    result.setRecognitionName(recognitionId);
                    logger.error("timeout intent recognize {}: {}",
                            recognitionId, e.getMessage(), e);
                    result.setStatus(RecognitionStatus.TIMEOUT);
                    result.setErrorMessage(e.getMessage());
                    return result;
                });
    }


    @Override
    public String getRecognitionId() {
        return recognitionId;
    }
}
