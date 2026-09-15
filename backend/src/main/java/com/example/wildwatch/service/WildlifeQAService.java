package com.example.wildwatch.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.wildwatch.dto.SpeciesSummaryDto;

@Service
public class WildlifeQAService {

    private final SpeciesQueryService speciesQueryService;
    private final ChatClient.Builder chatClientBuilder;

    public WildlifeQAService(SpeciesQueryService speciesQueryService, ChatClient.Builder chatClientBuilder) {
        this.speciesQueryService = speciesQueryService;
        this.chatClientBuilder = chatClientBuilder;
    }

    public String answerQuestion(String userQuestion, String region) {
        String question = userQuestion == null ? "" : userQuestion.trim();
        String targetRegion = region == null ? "" : region.trim();

        if (question.isBlank() || targetRegion.isBlank()) {
            throw new IllegalArgumentException("Both question and region are required.");
        }

        List<SpeciesSummaryDto> species = speciesQueryService.getSpeciesByRegion(targetRegion);
        String speciesList = species.isEmpty()
                ? "No observed species data available for this region."
                : species.stream()
                        .map(speciesSummary -> speciesSummary.scientificName() +
                                (speciesSummary.commonName() != null && !speciesSummary.commonName().isBlank()
                                        ? " (" + speciesSummary.commonName() + ")"
                                        : ""))
                        .toList()
                        .toString();

        String systemPrompt = "You are a wildlife expert assistant for WildWatch. Answer based only on this observed species data for the region: "
                + speciesList
                + ". If the question can't be answered from this data, say so honestly rather than guessing.";

        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }
}
