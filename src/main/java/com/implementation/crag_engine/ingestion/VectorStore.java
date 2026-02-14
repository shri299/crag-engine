package com.implementation.crag_engine.ingestion;

import java.util.List;
import java.util.Map;

public interface VectorStore {

	void add(List<Double> vector, Map<String, String> metadata);

	List<SearchResult> search(List<Double> queryVector, int topK);

	record SearchResult(Map<String, String> metadata, double score) {
	}

}
