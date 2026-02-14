package com.implementation.crag_engine.rag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RagController {

	private final RagService ragService;

	public RagController(RagService ragService) {
		this.ragService = ragService;
	}

	@GetMapping("/ask")
	public ResponseEntity<AnswerResponse> ask(@RequestParam("query") String query) {
		if (!StringUtils.hasText(query)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter must not be blank");
		}
		String answer = ragService.answer(query.trim());
		return ResponseEntity.ok(new AnswerResponse(answer));
	}

	public record AnswerResponse(String answer) {
	}

}
