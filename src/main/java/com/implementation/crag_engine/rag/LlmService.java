package com.implementation.crag_engine.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LlmService {

	private static final Logger logger = LoggerFactory.getLogger(LlmService.class);
	private static final String BASE_URL = "http://localhost:11434/api";
	private static final String MODEL = "phi3:mini";

	private final RestClient restClient;

	public LlmService(RestClient.Builder builder) {
		this.restClient = builder.baseUrl(BASE_URL).build();
	}

	public String generate(String prompt) {
		logger.debug("Calling LLM with prompt size {}", prompt.length());
		GenerationRequest request = new GenerationRequest(MODEL, prompt, false);
		GenerationResponse response = restClient.post()
			.uri("/generate")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.body(GenerationResponse.class);
		if (response == null || response.response() == null) {
			throw new IllegalStateException("LLM response was empty");
		}
		return response.response().trim();
	}

	private record GenerationRequest(String model, String prompt, boolean stream) {
	}

	private record GenerationResponse(String response) {
	}

}
