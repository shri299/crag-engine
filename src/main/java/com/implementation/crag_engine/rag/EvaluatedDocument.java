package com.implementation.crag_engine.rag;

import org.springframework.util.StringUtils;

public record EvaluatedDocument(String originalChunk, double score, Classification classification) {

	public EvaluatedDocument {
		if (!StringUtils.hasText(originalChunk)) {
			throw new IllegalArgumentException("originalChunk must not be blank");
		}
		if (Double.isNaN(score) || score < 0.0d || score > 1.0d) {
			throw new IllegalArgumentException("score must be between 0 and 1");
		}
		if (classification == null) {
			throw new IllegalArgumentException("classification must not be null");
		}
	}

	public enum Classification {
		CORRECT,
		AMBIGUOUS,
		INCORRECT
	}

}
