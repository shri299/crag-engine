package com.implementation.crag_engine.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryRewriter {

	private static final Logger logger = LoggerFactory.getLogger(QueryRewriter.class);

	private final LlmService llmService;

	public QueryRewriter(LlmService llmService) {
		this.llmService = llmService;
	}

	public String rewriteForWebSearch(String originalQuery) {
		if (!StringUtils.hasText(originalQuery)) {
			throw new IllegalArgumentException("originalQuery must not be blank");
		}
		try {
			String rewritten = llmService.generate(buildRewritePrompt(originalQuery)).trim();
			if (!StringUtils.hasText(rewritten)) {
				throw new IllegalStateException("LLM returned empty rewritten query");
			}
			logger.info("Rewritten query for web search: {}", rewritten);
			return rewritten;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to rewrite query for web search", ex);
		}
	}

	private String buildRewritePrompt(String originalQuery) {
		return """
				Rewrite the user query for web search.
				Requirements:
				- concise
				- preserve key entities
				- optimized for search engine usage
				Return only the rewritten query as plain text.
				
				Original query:
				%s
				""".formatted(originalQuery);
	}

}
