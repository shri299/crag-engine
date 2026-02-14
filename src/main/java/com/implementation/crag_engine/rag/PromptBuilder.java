package com.implementation.crag_engine.rag;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptBuilder {

	private static final String PROMPT_TEMPLATE = """
			You are a helpful assistant.
			Use the provided context to answer the question.
			If the answer is not present in the context, say you don't know.

			Context:
			---------
			%s
			---------

			Question:
			%s
			""";

	public String buildPrompt(List<String> contexts, String question) {
		String contextBlock = String.join(System.lineSeparator() + System.lineSeparator(), contexts);
		if (!StringUtils.hasText(contextBlock)) {
			contextBlock = "No relevant context available.";
		}
		return PROMPT_TEMPLATE.formatted(contextBlock, question);
	}

}
