package com.javan.smart.water.hook;

import com.javan.smart.water.common.constant.GraphConstant;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RagSearchAdvisor implements BaseAdvisor {

    public static final String DOCUMENT_CONTEXT = GraphConstant.RAG_DOCUMENT_CONTEXT_KEY;

    private final List<QueryTransformer> queryTransformers;

    private final QueryExpander queryExpander;

    private final VectorStore vectorStore;

    private final List<DocumentPostProcessor> documentPostProcessors;

    private final QueryAugmenter queryAugmenter;

    private final int order;

    public RagSearchAdvisor(List<QueryTransformer> queryTransformers,
                            QueryExpander queryExpander,
                            @Nullable VectorStore vectorStore,
                            List<DocumentPostProcessor> documentPostProcessors,
                            QueryAugmenter queryAugmenter, Integer order) {
        Assert.notNull(vectorStore, "milvusVectorStore must not be null");
        this.queryTransformers = queryTransformers;
        this.queryExpander = queryExpander;
        this.vectorStore = vectorStore;
        this.documentPostProcessors = documentPostProcessors;
        this.queryAugmenter = queryAugmenter != null ? queryAugmenter : ContextualQueryAugmenter.builder().build();
        this.order = order != null ? order : 0;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        // 1. Create a query from the user text, parameters, and conversation history.
        Query originalQuery = Query.builder()
                .text(chatClientRequest.prompt().getUserMessage().getText())
                .history(chatClientRequest.prompt().getInstructions())
                .context(context)
                .build();
        // 2. Apply query transformers
        Query transformedQuery = originalQuery;
        if (!CollectionUtils.isEmpty(this.queryTransformers)) {
            for (var queryTransformer : this.queryTransformers) {
                transformedQuery = queryTransformer.apply(transformedQuery);
            }
        }
        // 3. Expand query into one or multiple queries.
        List<Query> expandedQueries = this.queryExpander != null ? this.queryExpander.expand(transformedQuery)
                : List.of(transformedQuery);
        List<Document> allRetrievedDocuments = new ArrayList<>();
        for (Query query : expandedQueries) {
            List<Document> retrieveDocuments = vectorStore.similaritySearch(query.text());
            allRetrievedDocuments.addAll(retrieveDocuments);
        }
        // 4. Post-process the documents.
        List<Document> resultDocuments = new ArrayList<>();
        if (!CollectionUtils.isEmpty(documentPostProcessors)) {
            for (var documentPostProcessor : this.documentPostProcessors) {
                resultDocuments = documentPostProcessor.process(originalQuery, allRetrievedDocuments);
            }
        }
        context.put(DOCUMENT_CONTEXT, resultDocuments);
        // 5. Augment user query with the document contextual data.
        Query augmentedQuery = this.queryAugmenter.augment(originalQuery, resultDocuments);
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedQuery.text()))
                .context(context)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }
        chatResponseBuilder.metadata(DOCUMENT_CONTEXT, chatClientResponse.context().get(DOCUMENT_CONTEXT));
        return ChatClientResponse.builder()
                .chatResponse(chatResponseBuilder.build())
                .context(chatClientResponse.context())
                .build();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private List<QueryTransformer> queryTransformers = new ArrayList<>();

        private QueryExpander queryExpander;

        private VectorStore vectorStore;

        private List<DocumentPostProcessor> documentPostProcessors = new ArrayList<>();

        private QueryAugmenter queryAugmenter;

        private int order;


        private Builder() {
        }

        public Builder queryTransformers(List<QueryTransformer> queryTransformers) {
            this.queryTransformers = queryTransformers;
            return this;
        }

        public Builder queryExpander(QueryExpander queryExpander) {
            this.queryExpander = queryExpander;
            return this;
        }

        public Builder vectorStore(VectorStore vectorStore) {
            Assert.notNull(vectorStore, "vectorStore must not be null");
            this.vectorStore = vectorStore;
            return this;
        }

        public Builder documentPostProcessors(List<DocumentPostProcessor> documentPostProcessors) {
            this.documentPostProcessors = documentPostProcessors;
            return this;
        }

        public Builder queryAugmenter(QueryAugmenter queryAugmenter) {
            this.queryAugmenter = queryAugmenter;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }


        public RagSearchAdvisor build() {
            return new RagSearchAdvisor(queryTransformers, queryExpander, vectorStore,
                    documentPostProcessors, queryAugmenter, order);
        }
    }
}