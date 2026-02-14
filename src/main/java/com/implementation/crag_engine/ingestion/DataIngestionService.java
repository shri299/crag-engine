package com.implementation.crag_engine.ingestion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataIngestionService {

	private static final Logger logger = LoggerFactory.getLogger(DataIngestionService.class);

	private final DocumentLoader documentLoader;
	private final TextChunker textChunker;
	private final EmbeddingService embeddingService;
	private final VectorStore vectorStore;
	private final IngestionProperties properties;

	public DataIngestionService(DocumentLoader documentLoader, TextChunker textChunker, EmbeddingService embeddingService,
			VectorStore vectorStore, IngestionProperties properties) {
		this.documentLoader = documentLoader;
		this.textChunker = textChunker;
		this.embeddingService = embeddingService;
		this.vectorStore = vectorStore;
		this.properties = properties;
	}

	public void ingest() {
		String document = documentLoader.load(properties.getSourcePath());
		List<String> chunks = textChunker.chunk(document, properties.getChunkSize(), properties.getChunkOverlap());
		logger.info("Loaded {} characters and produced {} chunks", document.length(), chunks.size());
		AtomicInteger index = new AtomicInteger();
		int ingested = 0;
		for (String chunk : chunks) {
			vectorStore.add(embeddingService.embed(chunk), metadataFor(chunk, index.getAndIncrement()));
			ingested++;
		}
		logger.info("Vector store now holds {} embeddings", ingested);
	}

	private Map<String, String> metadataFor(String chunk, int chunkIndex) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("sourcePath", properties.getSourcePath());
		metadata.put("chunkIndex", Integer.toString(chunkIndex));
		metadata.put("text", chunk);
		return metadata;
	}

}
