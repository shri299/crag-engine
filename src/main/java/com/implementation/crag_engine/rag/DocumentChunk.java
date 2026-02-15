package com.implementation.crag_engine.rag;

import org.springframework.util.StringUtils;

public record DocumentChunk(String text, String source, String title) {

	public DocumentChunk {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException("text must not be blank");
		}
	}

}
