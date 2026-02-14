package com.implementation.crag_engine.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class DocumentLoader {

	private final ResourceLoader resourceLoader;

	public DocumentLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public String load(String resourcePath) {
		Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
		if (!resource.exists()) {
			throw new IllegalArgumentException("Resource not found in classpath: " + resourcePath);
		}
		try (InputStream inputStream = resource.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to read resource " + resourcePath, ex);
		}
	}

}
