package com.implementation.crag_engine.rag;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.implementation.crag_engine.ingestion.EmbeddingService;
import com.implementation.crag_engine.ingestion.VectorStore;

@Service
public class RagService {

	private static final Logger logger = LoggerFactory.getLogger(RagService.class);
	private static final int TOP_K = 3;
	private static final String FALLBACK_MESSAGE = "I could not find any relevant information in the knowledge base.";

	private final EmbeddingService embeddingService;
	private final VectorStore vectorStore;
	private final PromptBuilder promptBuilder;
	private final LlmService llmService;

	public RagService(EmbeddingService embeddingService, VectorStore vectorStore, PromptBuilder promptBuilder,
			LlmService llmService) {
		this.embeddingService = embeddingService;
		this.vectorStore = vectorStore;
		this.promptBuilder = promptBuilder;
		this.llmService = llmService;
	}

	public String answer(String query) {
		if (!StringUtils.hasText(query)) {
			throw new IllegalArgumentException("Query must not be blank");
		}
		try {
			List<Double> queryVector = embeddingService.embed(query);
			List<VectorStore.SearchResult> hits = vectorStore.search(queryVector, TOP_K);
			if (hits.isEmpty()) {
				logger.warn("No documents retrieved for query '{}'", query);
				return FALLBACK_MESSAGE;
			}
			List<String> contexts = hits.stream()
				.map(VectorStore.SearchResult::metadata)
				.map(metadata -> metadata.get("text"))
				.filter(Objects::nonNull)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
			if (contexts.isEmpty()) {
				logger.warn("Retrieved hits lacked usable context for query '{}'", query);
				return FALLBACK_MESSAGE;
			}
			String prompt = promptBuilder.buildPrompt(contexts, query);
			return llmService.generate(prompt);
		}
		catch (Exception ex) {
			logger.error("Failed to process query '{}'", query, ex);
			return "An error occurred while processing your request. Please try again.";
		}
	}

}
