package com.javan.smart.water.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.javan.smart.water.agent.analysis.AnalysisAgent;
import com.javan.smart.water.agent.router.intent.IIntentRecognitionProvider;
import com.javan.smart.water.agent.work.HandleOrderAgent;
import com.javan.smart.water.auth.IRbacService;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.constant.GraphRouterConstant;
import com.javan.smart.water.graph.dispatcher.IntentDispatcher;
import com.javan.smart.water.graph.dispatcher.NextDispatcher;
import com.javan.smart.water.graph.node.*;
import com.javan.smart.water.view.service.WaterAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;

@Configuration
public class WaterGraphConfig {

    private static final Logger logger = LoggerFactory.getLogger(WaterGraphConfig.class);

    @Value("classpath:/prompts/analysis-intent-prompt.md")
    private Resource intentPromptResource;

    @Bean
    public StateGraph smartWaterOperationGraph(
            AnalysisAgent analysisAgent,
            HandleOrderAgent handleOrderAgent,
            IIntentRecognitionProvider intentRecognitionProvider,
            IRbacService rbacService,
            VectorStore vectorStore,
            ChatClient.Builder chatBuilder,
            WaterAnalysisService waterAnalysisService
    ) throws GraphStateException {
        logger.info("Initializing smartWaterOperationGraph");

        // 意图识别
        IntentNode intentNode = new IntentNode(intentRecognitionProvider, rbacService);
        // QA
        QARagRetrieveNode ragNode = new QARagRetrieveNode(vectorStore, chatBuilder);
        // 闲聊
        ChatNode chatNode = ChatNode.create("chat_node", chatBuilder, null);
        //  水质分析补充信息
        TaskInfoWaitNode taskInfoWaitNode = new TaskInfoWaitNode(waterAnalysisService);
        // 水质分析节点
        AnalysisNode analysisNode = new AnalysisNode(analysisAgent, waterAnalysisService);
        // 水质分析人工补充
        AnalysisIntentNode analysisIntentNode = new AnalysisIntentNode(chatBuilder, intentPromptResource);
        ClarificationNode clarificationNode = new ClarificationNode(chatBuilder, null);
        // 创建工单
        HandleOrderNode orderNode = new HandleOrderNode(handleOrderAgent, rbacService);

        StateGraph graph = new StateGraph("SMART WATER AGENT", WaterStateFactory.get())
                .addNode(GraphRouterConstant.INTENT, intentNode)
                .addNode(GraphRouterConstant.WATER_QA, node_async(ragNode))
                .addNode(GraphRouterConstant.CHAT, node_async(chatNode))
                .addNode(GraphRouterConstant.ANALYSIS_TASK_WAIT, taskInfoWaitNode)
                .addNode(GraphRouterConstant.WATER_ANALYSIS, node_async(analysisNode))
                .addNode(GraphRouterConstant.WATER_ORDER, orderNode)
                .addNode(GraphRouterConstant.ANALYSIS_INTENT, analysisIntentNode)
                .addNode(GraphRouterConstant.WATER_ANALYSIS_MAKE_UP, clarificationNode)
                .addEdge(StateGraph.START, GraphRouterConstant.INTENT)
                .addConditionalEdges(GraphRouterConstant.INTENT, AsyncEdgeAction.edge_async(IntentDispatcher.create()), Map.of(
                        GraphRouterConstant.CHAT, GraphRouterConstant.CHAT,
                        GraphRouterConstant.ANALYSIS_TASK_WAIT, GraphRouterConstant.ANALYSIS_TASK_WAIT,
                        GraphRouterConstant.WATER_QA, GraphRouterConstant.WATER_QA,
                        StateGraph.END, StateGraph.END
                ))
                .addConditionalEdges(GraphRouterConstant.ANALYSIS_TASK_WAIT, AsyncEdgeAction.edge_async(NextDispatcher.create())
                        , Map.of(
                                GraphRouterConstant.CHAT, GraphRouterConstant.CHAT,
                                GraphRouterConstant.WATER_ANALYSIS, GraphRouterConstant.WATER_ANALYSIS,
                                StateGraph.END, StateGraph.END
                        ))
                .addConditionalEdges(GraphRouterConstant.WATER_ANALYSIS, edge_async(state -> {
                            String nextNode = state.value(GraphRouterConstant.NEXT_NODE_KEY).map(Object::toString).orElse("");
                            if (StrUtil.isNotBlank(nextNode)) {
                                return nextNode;
                            }
                            String userId = state.value(GraphConstant.USER_ID_KEY).map(Object::toString).orElse("");
                            if (rbacService.isAdmin(userId)) {
                                return GraphRouterConstant.WATER_ORDER;
                            } else {
                                return StateGraph.END;
                            }
                        }), Map.of(
                                GraphRouterConstant.WATER_ORDER, GraphRouterConstant.WATER_ORDER,
                                StateGraph.END, StateGraph.END,
                                GraphRouterConstant.ANALYSIS_INTENT, GraphRouterConstant.ANALYSIS_INTENT
                        )
                )
                .addConditionalEdges(GraphRouterConstant.ANALYSIS_INTENT,
                        AsyncEdgeAction.edge_async(NextDispatcher.create()), Map.of(
                                GraphRouterConstant.WATER_ANALYSIS, GraphRouterConstant.WATER_ANALYSIS,
                                GraphRouterConstant.WATER_ANALYSIS_MAKE_UP, GraphRouterConstant.WATER_ANALYSIS_MAKE_UP,
                                StateGraph.END, StateGraph.END))
                .addEdge(GraphRouterConstant.WATER_ANALYSIS_MAKE_UP, GraphRouterConstant.ANALYSIS_INTENT)
                .addEdge(GraphRouterConstant.WATER_ORDER, StateGraph.END)
                .addEdge(GraphRouterConstant.WATER_QA, StateGraph.END)
                .addEdge(GraphRouterConstant.CHAT, StateGraph.END);

        printGraphRepresentation(graph, "SMART WATER AGENT GRAPH");

        logger.info("smartWaterOperationGraph initialized");
        return graph;
    }

    private void printGraphRepresentation(StateGraph graph, String graphName) {
        try {
            GraphRepresentation representation = graph.getGraph(
                    GraphRepresentation.Type.PLANTUML,
                    graphName
            );
            logger.info("\n========== {} ==========\n{}\n====================\n",
                    graphName, representation.content());
        } catch (Exception e) {
            logger.warn("Failed to generate graph representation: {}", e.getMessage());
        }
    }


    /**
     * 加入可观测性
     *
     * @author Javan
     * @since 1.0.0
     */
    @Bean
    public CompiledGraph compiledWaterOpsGraph(StateGraph waterGraph, CompileConfig config) throws GraphStateException {
        config.setStore(new MemoryStore());
        return waterGraph
                .compile(config);
    }


    public static class WaterStateFactory {

        public static KeyStrategyFactory get() {
            return new KeyStrategyFactoryBuilder()
                    .defaultStrategy(KeyStrategy.REPLACE)
                    .addStrategy(GraphConstant.INPUT_KEY)
                    .addStrategy(GraphConstant.INTENT_TYPE)
                    .addStrategy(GraphConstant.OUTPUT_KEY, KeyStrategy.APPEND)
                    .addStrategy(GraphConstant.AGENT_COT_ENABLE_KEY)
                    .addStrategy(GraphConstant.AGENT_AUTH_STATUS_KEY)
                    .addStrategy(GraphConstant.USER_ID_KEY)
                    .addStrategy(GraphConstant.ANALYSIS_REPORT_OUTPUT_KEY)
                    .addStrategy(GraphConstant.RAG_DOCUMENT_CONTEXT_KEY)
                    .addStrategy(GraphConstant.AGENT_AUTH_DENIED_REASON_KEY)
                    .addStrategy(GraphConstant.RENDER_TYPE)
                    .addStrategy(GraphConstant.MAKE_UP_KEY)
                    .addStrategy(GraphConstant.MAKE_UP_CONTENT_MAP_KEY)
                    .addStrategy(GraphRouterConstant.NEXT_NODE_KEY)
                    .addStrategy(GraphConstant.USER_REPLY_KEY)
                    .addStrategy(GraphConstant.FORCE_END_KEY)
                    .addStrategy(GraphConstant.FORCE_END_REASON_KEY)
                    .addStrategy(GraphConstant.ANALYSIS_INTENT_KEY)
                    .addStrategy(GraphConstant.ANALYSIS_INTENT_REASON_KEY)
                    .addStrategy(GraphConstant.ANALYSIS_WATER_DEPEND_KEY)
                    .build();
        }
    }
}
