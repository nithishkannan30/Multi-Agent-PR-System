package com.example.MultiAgentsForPR.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class HuggingFaceEmbeddingModel implements EmbeddingModel {

    private final RestClient restClient;
    private final String model;

    public HuggingFaceEmbeddingModel(@Value("${huggingface.api-key}") String apiKey,
                                     @Value("${huggingface.embedding-model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://router.huggingface.co")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        int index = 0;
        for (String text : request.getInstructions()) {
            embeddings.add(new Embedding(safeEmbed(text), index++));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(String text) {
        return safeEmbed(text);
    }

    @Override
    public float[] embed(Document document) {
        return safeEmbed(document.getText());
    }

    private float[] safeEmbed(String text) {
        try {
            return embedText(text);
        } catch (Exception e) {
            System.err.println("Embedding failed after retries, using zero vector: " + e.getMessage());
            return new float[384];
        }
    }

    @SuppressWarnings("unchecked")
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    private float[] embedText(String text) {
        List<Double> response = restClient.post()
                .uri("/hf-inference/models/{model}/pipeline/feature-extraction", model)
                .body(java.util.Map.of("inputs", text, "options", java.util.Map.of("wait_for_model", true)))
                .retrieve()
                .body(List.class);

        float[] vector = new float[response.size()];
        for (int i = 0; i < response.size(); i++) {
            vector[i] = response.get(i).floatValue();
        }
        return vector;
    }
}