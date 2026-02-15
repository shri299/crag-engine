package com.implementation.crag_engine.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeRefiner {

	private static final Logger logger = LoggerFactory.getLogger(KnowledgeRefiner.class);
	private static final int SENTENCE_GROUP_SIZE = 2;
	private static final String SENTENCE_SPLIT_REGEX = "(?<=[.!?])\\s+";

	private final LlmService llmService;

	public KnowledgeRefiner(LlmService llmService) {
		this.llmService = llmService;
	}

	public String refine(String query, String chunk) {
		if (!StringUtils.hasText(query)) {
			throw new IllegalArgumentException("query must not be blank");
		}
		if (!StringUtils.hasText(chunk)) {
			throw new IllegalArgumentException("chunk must not be blank");
		}
		try {
			List<String> strips = splitIntoStrips(chunk);
			List<String> compatible = new ArrayList<>();
			for (String strip : strips) {
				if (isCompatible(query, strip)) {
					compatible.add(strip);
				}
			}
			logger.info("Knowledge refinement retained {}/{} strips for query='{}'", compatible.size(), strips.size(),
				query);
			return String.join("\n", compatible);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to refine knowledge", ex);
		}
	}

	private List<String> splitIntoStrips(String chunk) {
		String[] rawSentences = chunk.split(SENTENCE_SPLIT_REGEX);
		List<String> sentences = new ArrayList<>();
		for (String sentence : rawSentences) {
			if (StringUtils.hasText(sentence)) {
				sentences.add(sentence.trim());
			}
		}
		if (sentences.isEmpty()) {
			return List.of(chunk.trim());
		}
		List<String> strips = new ArrayList<>();
		for (int i = 0; i < sentences.size(); i += SENTENCE_GROUP_SIZE) {
			int end = Math.min(i + SENTENCE_GROUP_SIZE, sentences.size());
			strips.add(String.join(" ", sentences.subList(i, end)));
		}
		return strips;
	}

	private boolean isCompatible(String query, String strip) {
		String response = llmService.generate(buildCompatibilityPrompt(query, strip));
		String normalized = response == null ? "" : response.trim().toLowerCase(Locale.ROOT);
		if (normalized.startsWith("true")) {
			return true;
		}
		if (normalized.startsWith("false")) {
			return false;
		}
		throw new IllegalStateException("Could not parse compatibility result: " + response);
	}

	private String buildCompatibilityPrompt(String query, String strip) {
		return """
				Determine whether the strip is compatible with the user query.
				Return exactly one word: true or false.
				
				Query:
				%s
				
				Strip:
				%s
				""".formatted(query, strip);
	}

}
