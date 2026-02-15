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
	private static final String REFINEMENT_FALLBACK_MESSAGE = "Retrieved documents did not produce compatible refined context.";
	private static final String WEB_SEARCH_FALLBACK_MESSAGE = "Unable to retrieve relevant information from web search.";
	private static final String AMBIGUOUS_FALLBACK_MESSAGE = "Unable to retrieve sufficiently relevant information.";

	private final EmbeddingService embeddingService;
	private final VectorStore vectorStore;
	private final PromptBuilder promptBuilder;
	private final LlmService llmService;
	private final RetrievalEvaluator retrievalEvaluator;
	private final KnowledgeRefiner knowledgeRefiner;
	private final QueryRewriter queryRewriter;
	private final WebSearchService webSearchService;

	public RagService(EmbeddingService embeddingService, VectorStore vectorStore, PromptBuilder promptBuilder,
			LlmService llmService, RetrievalEvaluator retrievalEvaluator, KnowledgeRefiner knowledgeRefiner,
			QueryRewriter queryRewriter, WebSearchService webSearchService) {
		this.embeddingService = embeddingService;
		this.vectorStore = vectorStore;
		this.promptBuilder = promptBuilder;
		this.llmService = llmService;
		this.retrievalEvaluator = retrievalEvaluator;
		this.knowledgeRefiner = knowledgeRefiner;
		this.queryRewriter = queryRewriter;
		this.webSearchService = webSearchService;
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
			OverallRetrievalDecision decision = retrievalEvaluator.evaluateOverall(contexts, query);
			return switch (decision.decision()) {
				case INCORRECT -> answerWithWebSearch(query);
				case CORRECT -> answerFromRetrievedContext(query, decision.docsAboveLowerThreshold());
				case AMBIGUOUS -> answerFromAmbiguousContext(query, decision);
			};
		}
		catch (Exception ex) {
			logger.error("Failed to process query '{}'", query, ex);
			return "An error occurred while processing your request. Please try again.";
		}
	}

	private String answerFromAmbiguousContext(String originalQuery, OverallRetrievalDecision decision) {
		List<EvaluatedDocument> goodDocs = decision.goodDocs();
		List<EvaluatedDocument> badDocs = decision.badDocs();
		int totalDocs = decision.highDocs().size() + goodDocs.size() + badDocs.size();
		logger.info("Ambiguous workflow doc split totalDocs={} goodDocs={} badDocs={}", totalDocs, goodDocs.size(),
			badDocs.size());

		List<String> combinedContexts = goodDocs.stream()
			.map(EvaluatedDocument::originalChunk)
			.collect(Collectors.toList());

		if (!badDocs.isEmpty()) {
			logger.info("Ambiguous workflow triggering web search supplementation for query='{}'", originalQuery);
			String rewrittenQuery = queryRewriter.rewriteForWebSearch(originalQuery);
			List<DocumentChunk> webResults = webSearchService.search(rewrittenQuery);
			combinedContexts.addAll(webResults.stream().map(DocumentChunk::text).toList());
		}
		else {
			logger.info("Ambiguous workflow skipping web search because badDocs is empty for query='{}'", originalQuery);
		}

		logger.info("Ambiguous workflow combined doc count={} for query='{}'", combinedContexts.size(), originalQuery);
		if (combinedContexts.isEmpty()) {
			return AMBIGUOUS_FALLBACK_MESSAGE;
		}
		List<String> refinedCombinedContexts = combinedContexts.stream()
			.map(text -> knowledgeRefiner.refine(originalQuery, text))
			.filter(StringUtils::hasText)
			.collect(Collectors.toList());
		logger.info("Ambiguous workflow refined doc count={} for query='{}'", refinedCombinedContexts.size(),
			originalQuery);
		if (refinedCombinedContexts.isEmpty()) {
			return AMBIGUOUS_FALLBACK_MESSAGE;
		}
		String prompt = promptBuilder.buildPrompt(refinedCombinedContexts, originalQuery);
		logger.info("Ambiguous workflow final context size={} chars for query='{}'", prompt.length(), originalQuery);
		return llmService.generate(prompt);
	}

	private String answerFromRetrievedContext(String query, List<EvaluatedDocument> selectedDocuments) {
		List<String> refinedContexts = selectedDocuments.stream()
			.map(EvaluatedDocument::originalChunk)
			.map(text -> knowledgeRefiner.refine(query, text))
			.filter(StringUtils::hasText)
			.collect(Collectors.toList());
		if (refinedContexts.isEmpty()) {
			logger.warn("No compatible refined contexts produced from local retrieval for query '{}'", query);
			return REFINEMENT_FALLBACK_MESSAGE;
		}
		String prompt = promptBuilder.buildPrompt(refinedContexts, query);
		return llmService.generate(prompt);
	}

	private String answerWithWebSearch(String originalQuery) {
		String rewrittenQuery = queryRewriter.rewriteForWebSearch(originalQuery);
		List<DocumentChunk> webResults = webSearchService.search(rewrittenQuery);
		if (webResults.isEmpty()) {
			logger.warn("No web results found after query rewrite for query '{}'", originalQuery);
			return WEB_SEARCH_FALLBACK_MESSAGE;
		}
		List<String> refinedWebContexts = webResults.stream()
			.map(DocumentChunk::text)
			.map(text -> knowledgeRefiner.refine(originalQuery, text))
			.filter(StringUtils::hasText)
			.collect(Collectors.toList());
		logger.info("Refined web contexts count={} for query='{}'", refinedWebContexts.size(), originalQuery);
		if (refinedWebContexts.isEmpty()) {
			return WEB_SEARCH_FALLBACK_MESSAGE;
		}
		String prompt = promptBuilder.buildPrompt(refinedWebContexts, originalQuery);
		return llmService.generate(prompt);
	}

}
