package com.implementation.crag_engine.ingestion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EmbeddingService {

	private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

	private final RestClient restClient;
	private final IngestionProperties properties;

	public EmbeddingService(RestClient.Builder builder, IngestionProperties properties) {
		this.restClient = builder.baseUrl(properties.getEmbedding().getBaseUrl()).build();
		this.properties = properties;
	}

	public List<Double> embed(String text) {
		logger.debug("Requesting embedding for chunk of size {}", text.length());
		EmbeddingRequest request = new EmbeddingRequest(properties.getEmbedding().getModel(), text);
		EmbeddingResponse response = restClient.post()
			.uri("")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.body(EmbeddingResponse.class);
		if (response.embedding() == null || response.embedding().isEmpty()) {
			throw new IllegalStateException("Embedding service returned no data");
		}
		return response.embedding();
	}

	private record EmbeddingRequest(String model, String input) {
	}

	private record EmbeddingResponse(List<Double> embedding) {
	}

}
