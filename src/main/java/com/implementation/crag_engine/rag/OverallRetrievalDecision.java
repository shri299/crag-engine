package com.implementation.crag_engine.rag;

import java.util.ArrayList;
import java.util.List;

public record OverallRetrievalDecision(Decision decision, List<EvaluatedDocument> goodDocs,
		List<EvaluatedDocument> badDocs, List<EvaluatedDocument> highDocs) {

	public OverallRetrievalDecision {
		if (decision == null) {
			throw new IllegalArgumentException("decision must not be null");
		}
		goodDocs = goodDocs == null ? List.of() : List.copyOf(goodDocs);
		badDocs = badDocs == null ? List.of() : List.copyOf(badDocs);
		highDocs = highDocs == null ? List.of() : List.copyOf(highDocs);
	}

	public List<EvaluatedDocument> docsAboveLowerThreshold() {
		List<EvaluatedDocument> docs = new ArrayList<>(highDocs.size() + goodDocs.size());
		docs.addAll(highDocs);
		docs.addAll(goodDocs);
		return docs;
	}

	public enum Decision {
		CORRECT,
		AMBIGUOUS,
		INCORRECT
	}

}
