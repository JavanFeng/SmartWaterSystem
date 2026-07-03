/*
 * Copyright 2025 the original author or authors.
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

package com.javan.smart.water.rag.config;


import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 测试
 *
 * @author Javan
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(VectorCreateProps.class)
@ConditionalOnProperty(prefix = "water.qa.vector.init", name = "enable", havingValue = "true",
        matchIfMissing = false)
public class VectorDataInit implements ApplicationRunner {

    private final Logger logger = LoggerFactory.getLogger(VectorDataInit.class);

    private final MilvusVectorStore vectorStore;

    private final VectorCreateProps vectorCreateProps;
    private final MilvusVectorStoreProperties milvusVectorStoreProperties;

    public VectorDataInit(MilvusVectorStore vectorStore, VectorCreateProps vectorCreateProps
            , MilvusVectorStoreProperties properties) {
        this.vectorStore = vectorStore;
        this.vectorCreateProps = vectorCreateProps;
        this.milvusVectorStoreProperties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {

        List<Document> documents = List.of(
                // ===== 水质监测核心指标 =====
                new Document("pH值：反映水体酸碱度，地表水正常范围通常在6.5~8.5之间，过低或过高均会影响水生生物生存及管网腐蚀。", Map.of("cate", "qa", "alc", "all")),
                new Document("溶解氧（DO）：衡量水体自净能力的重要指标，低于2mg/L时水质严重恶化，鱼类难以生存。", Map.of("cate", "qa", "alc", "all")),
                new Document("浊度（Turbidity）：表示水中悬浮物对光线透过时的阻碍程度，是饮用水安全的重要感官指标。", Map.of("cate", "qa", "alc", "all")),
                new Document("化学需氧量（COD）：反映水中受还原性物质污染的程度，COD超标通常意味着水体受到有机物污染。", Map.of("cate", "qa", "alc", "all")),
                new Document("氨氮（NH3-N）：主要来源于生活污水和农业面源污染，是评价水体富营养化和水质恶化的关键指标。", Map.of("cate", "qa", "alc", "all")),
                new Document("电导率：间接反映水中溶解性盐类的总量，数值越高说明水体中溶解的离子越多，污染可能越严重。", Map.of("cate", "qa", "alc", "all")),
                new Document("水温：水体的基本物理性质，水温变化会影响水中溶解氧含量、化学反应速率及水生生物的代谢。", Map.of("cate", "qa", "alc", "all")),

                // ===== 水质异常与处理知识 =====
                new Document("水体富营养化：由于氮、磷等营养物质过量导致藻类异常繁殖，消耗水中溶解氧，引发水华或赤潮现象。", Map.of("cate", "qa", "alc", "all")),
                new Document("重金属污染：如铅、汞、镉等，具有生物累积性和不可降解性，需通过特定的化学沉淀或离子交换工艺去除。", Map.of("cate", "qa", "alc", "all")),
                new Document("余氯标准：自来水出厂水余氯通常需保持在0.3~4.0mg/L，以确保在管网输送过程中持续杀菌消毒。", Map.of("cate", "qa", "alc", "all")),
                new Document("总磷（TP）控制：总磷是引起水体富营养化的限制性因素，污水处理厂通常采用生物除磷结合化学沉淀法进行深度处理。", Map.of("cate", "qa", "alc", "all")),
                new Document("亚硝酸盐氮：是氮循环的中间产物，具有生物毒性，饮用水中需严格控制其浓度限值。", Map.of("cate", "qa", "alc", "all")),

                // ===== 水处理与监测工艺 =====
                new Document("常规净水工艺：包括混凝、沉淀、过滤和消毒四个基本步骤，用于去除水中的悬浮物和病原微生物。", Map.of("cate", "qa", "alc", "all")),
                new Document("在线监测设备：水质自动监测站通常包含采样单元、预处理单元、分析仪表和数据传输单元，实现24小时实时预警。", Map.of("cate", "qa", "alc", "all")),
                new Document("深度处理技术：针对微污染水源，常采用臭氧-生物活性炭（O3-BAC）工艺，有效去除水中微量有机物和异味。", Map.of("cate", "qa", "alc", "all")),
                new Document("水质采样规范：测定金属元素的水样需酸化保存，测定有机物的水样需低温避光保存，部分项目需现场固定后尽快送检。", Map.of("cate", "qa", "alc", "all"))
        );
        if (vectorCreateProps.isAutoCreate()) {
            createCollections();
            // 可能需要等待一段时间，确保集合创建完成
        }

        List<Document> hits = vectorStore.similaritySearch("重金属污染");
        if (hits.isEmpty()) {
            // 避免重复 消耗token，每10条一批添加
            int batchSize = 10;
            for (int i = 0; i < documents.size(); i += batchSize) {
                int end = Math.min(i + batchSize, documents.size());
                List<Document> batch = documents.subList(i, end);
                vectorStore.add(batch);
                logger.info("Vector data batch initialized: {}/{}", end, documents.size());
            }
            logger.info("All vector data initialized, total: {}", documents.size());
        }
    }

    private void createCollections() {
        MilvusServiceClient client = vectorStore.<MilvusServiceClient>getNativeClient().orElse(null);


        R<Boolean> hasCollection = client.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(milvusVectorStoreProperties.getCollectionName())
                        .withDatabaseName(milvusVectorStoreProperties.getDatabaseName())
                        .build());
        if (hasCollection.getData()) {
            client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(milvusVectorStoreProperties.getCollectionName())
                    .withDatabaseName(milvusVectorStoreProperties.getDatabaseName())
                    .build());
            logger.info("数据库{}中已经存在该collections：{}不再创建。", milvusVectorStoreProperties.getDatabaseName(), milvusVectorStoreProperties.getCollectionName());
        }

        CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                .addFieldType(FieldType.newBuilder()
                        .withAutoID(true)
                        .withName("doc_id")
                        .withPrimaryKey(true)
                        .withDataType(DataType.Int64)
                        .build())
                .addFieldType(FieldType.newBuilder()
                        .withName("embedding")
                        .withDataType(DataType.FloatVector)
                        .withDimension(1024)
                        .build())
                .addFieldType(FieldType.newBuilder()
                        .withName("content")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(2048).build())
                .addFieldType(FieldType.newBuilder()
                        .withName("metadata")
                        .withDataType(DataType.JSON)
                        .build()).build();


        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withSchema(schema)
                .withCollectionName(milvusVectorStoreProperties.getCollectionName())
                .withDatabaseName(milvusVectorStoreProperties.getDatabaseName())
                .withShardsNum(1)
                // 测试没必要
                .withProperty(Constant.MMAP_ENABLED, "false")
                .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED).build();

        R<RpcStatus> collection = client.createCollection(param);
        logger.info("Create collection status: {}", collection.getData().getMsg());

        R<RpcStatus> docVector = client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(milvusVectorStoreProperties.getCollectionName())
                .withDatabaseName(milvusVectorStoreProperties.getDatabaseName())
                .withFieldName("embedding")
                .withIndexType(IndexType.AUTOINDEX)
                .withMetricType(MetricType.COSINE)
                .build());
        logger.info("Create index status 'embedding': {}", docVector.getData().getMsg());


        R<RpcStatus> idVector = client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(milvusVectorStoreProperties.getCollectionName())
                .withDatabaseName(milvusVectorStoreProperties.getDatabaseName())
                .withFieldName("doc_id")
                .withIndexType(IndexType.AUTOINDEX)
                .build());
        logger.info("Create index status 'doc_id': {}", idVector.getData().getMsg());

        client.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(milvusVectorStoreProperties.getCollectionName())
                .withDatabaseName(milvusVectorStoreProperties.getDatabaseName())
                .withRefresh(true)
                .build());
    }

}
