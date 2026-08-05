package com.ezdo.service;

import com.ezdo.dto.RelatedGoalMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalVectorService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public void indexGoal(UUID goalId, UUID userId, String title, String description) {
        String text = description != null ? title + ". " + description : title;

        Document document = new Document(
                goalId.toString(),
                text,
                Map.of(
                        "goalId", goalId.toString(),
                        "userId", userId.toString()
                )
        );

        vectorStore.add(List.of(document));
    }

    public List<RelatedGoalMatch> findSimilarGoals(String queryText, UUID userId, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(queryText)
                .topK(topK)
                .similarityThreshold(0.75)
                .filterExpression("userId == '" + userId + "'")
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        return results.stream()
                .map(doc -> new RelatedGoalMatch(
                        UUID.fromString(doc.getMetadata().get("goalId").toString()),
                        doc.getText(),
                        doc.getScore()
                ))
                .toList();
    }


    }
