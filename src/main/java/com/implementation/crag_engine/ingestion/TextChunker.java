package com.implementation.crag_engine.ingestion;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TextChunker {

	public List<String> chunk(String text, int chunkSize, int overlap) {
		if (!StringUtils.hasText(text)) {
			return List.of();
		}
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize must be positive");
		}
		if (overlap < 0 || overlap >= chunkSize) {
			throw new IllegalArgumentException("overlap must be in range [0, chunkSize)");
		}

		List<String> chunks = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(start + chunkSize, text.length());
			chunks.add(text.substring(start, end));
			if (end == text.length()) {
				break;
			}
			start = end - overlap;
		}
		return chunks;
	}

}
