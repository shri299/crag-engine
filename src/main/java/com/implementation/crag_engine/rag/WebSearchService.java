package com.implementation.crag_engine.rag;

import java.util.List;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

@Service
public class WebSearchService {

	private static final Logger logger = LoggerFactory.getLogger(WebSearchService.class);
	private static final int TOP_K = 3;
	private static final String WIKIPEDIA_BASE_URL = "https://en.wikipedia.org";
	private static final String HTML_TAG_REGEX = "<[^>]*>";

	private final RestClient restClient;

	public WebSearchService(RestClient.Builder builder) {
		this.restClient = builder.baseUrl(WIKIPEDIA_BASE_URL).build();
	}

	public List<DocumentChunk> search(String query) {
		if (!StringUtils.hasText(query)) {
			throw new IllegalArgumentException("query must not be blank");
		}
		try {
			WikipediaSearchResponse response = restClient.get()
				.uri(uriBuilder -> buildSearchUri(uriBuilder, query))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(WikipediaSearchResponse.class);
			if (response == null || response.query() == null || response.query().search() == null) {
				logger.warn("Web search API returned empty response for query='{}'", query);
				return List.of();
			}
			List<DocumentChunk> chunks = response.query()
				.search()
				.stream()
				.limit(TOP_K)
				.map(this::toDocumentChunk)
				.toList();
			logger.info("Web search returned {} results for query='{}'", chunks.size(), query);
			return chunks;
		}
		catch (Exception ex) {
			logger.error("Web search failed for query='{}'", query, ex);
			return List.of();
		}
	}

	private URI buildSearchUri(UriBuilder uriBuilder, String query) {
		return uriBuilder.path("/w/api.php")
			.queryParam("action", "query")
			.queryParam("list", "search")
			.queryParam("format", "json")
			.queryParam("utf8", 1)
			.queryParam("srlimit", TOP_K)
			.queryParam("srsearch", query)
			.build();
	}

	private DocumentChunk toDocumentChunk(WikipediaSearchResult result) {
		String title = StringUtils.hasText(result.title()) ? result.title().trim() : "Untitled";
		String snippet = sanitizeSnippet(result.snippet());
		String text = "Title: " + title + System.lineSeparator() + "Snippet: " + snippet;
		String source = "https://en.wikipedia.org/wiki/" + title.replace(' ', '_');
		return new DocumentChunk(text, source, title);
	}

	private String sanitizeSnippet(String snippet) {
		if (!StringUtils.hasText(snippet)) {
			return "";
		}
		return snippet.replaceAll(HTML_TAG_REGEX, "").replaceAll("\\s+", " ").trim();
	}

	private record WikipediaSearchResponse(WikipediaQuery query) {
	}

	private record WikipediaQuery(List<WikipediaSearchResult> search) {
	}

	private record WikipediaSearchResult(String title, String snippet) {
	}

}
