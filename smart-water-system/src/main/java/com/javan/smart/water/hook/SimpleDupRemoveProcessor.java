package com.javan.smart.water.hook;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author FengJ
 * @description 簡單去重
 */
public class SimpleDupRemoveProcessor implements DocumentPostProcessor {
    @Override
    public List<Document> process(Query query, List<Document> documents) {
        // 根据id去重，根据score排序从大到小
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return documents.stream()
                .filter(doc -> doc.getId() != null)
                .filter(distinctByKey(Document::getId, seen))
                .sorted(Comparator.comparing(Document::getScore).reversed())
                .toList();
    }

    private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor, Set<Object> seen) {
        return t -> seen.add(keyExtractor.apply(t));
    }
}
