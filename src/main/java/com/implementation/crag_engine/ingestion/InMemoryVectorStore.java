package com.implementation.crag_engine.ingestion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class InMemoryVectorStore implements VectorStore {

	private final List<StoredVector> vectors = new ArrayList<>();

	@Override
	public synchronized void add(List<Double> vector, Map<String, String> metadata) {
		Objects.requireNonNull(vector, "vector must not be null");
		if (vector.isEmpty()) {
			throw new IllegalArgumentException("vector must not be empty");
		}
		Objects.requireNonNull(metadata, "metadata must not be null");
		vectors.add(new StoredVector(new ArrayList<>(vector), new HashMap<>(metadata)));
	}

	@Override
	public synchronized List<SearchResult> search(List<Double> queryVector, int topK) {
		if (vectors.isEmpty() || queryVector == null || queryVector.isEmpty() || topK <= 0) {
			return List.of();
		}
		return vectors.stream()
			.map(vector -> new SearchResult(vector.metadata, cosineSimilarity(queryVector, vector.vector)))
			.sorted(Comparator.comparingDouble(SearchResult::score).reversed())
			.limit(topK)
			.collect(Collectors.toList());
	}

	private double cosineSimilarity(List<Double> left, List<Double> right) {
		if (left.size() != right.size()) {
			throw new IllegalArgumentException("Vector dimensions must match");
		}
		double dotProduct = 0;
		double leftMagnitude = 0;
		double rightMagnitude = 0;
		for (int i = 0; i < left.size(); i++) {
			double l = left.get(i);
			double r = right.get(i);
			dotProduct += l * r;
			leftMagnitude += l * l;
			rightMagnitude += r * r;
		}
		if (leftMagnitude == 0 || rightMagnitude == 0) {
			return 0;
		}
		return dotProduct / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
	}

	private record StoredVector(List<Double> vector, Map<String, String> metadata) {
	}

}
