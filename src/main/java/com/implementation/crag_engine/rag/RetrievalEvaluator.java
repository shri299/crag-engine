package com.implementation.crag_engine.rag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RetrievalEvaluator {

	private static final Logger logger = LoggerFactory.getLogger(RetrievalEvaluator.class);
	private static final Pattern SCORE_PATTERN = Pattern.compile("(?i)score\\s*[:=]\\s*(0(?:\\.\\d+)?|1(?:\\.0+)?)");

	private final LlmService llmService;
	private final double upperThreshold;
	private final double lowerThreshold;

	public RetrievalEvaluator(LlmService llmService,
			@Value("${rag.retrieval.upper-threshold:0.8}") double upperThreshold,
			@Value("${rag.retrieval.lower-threshold:0.3}") double lowerThreshold) {
		this.llmService = llmService;
		if (lowerThreshold < 0.0d || upperThreshold > 1.0d || lowerThreshold >= upperThreshold) {
			throw new IllegalArgumentException(
					"Invalid retrieval thresholds: require 0 <= lower-threshold < upper-threshold <= 1");
		}
		this.upperThreshold = upperThreshold;
		this.lowerThreshold = lowerThreshold;
	}

	public OverallRetrievalDecision evaluateOverall(List<String> chunks, String query) {
		if (chunks == null || chunks.isEmpty()) {
			throw new IllegalArgumentException("chunks must not be empty");
		}
		List<EvaluatedDocument> evaluatedDocuments = chunks.stream().map(chunk -> evaluate(query, chunk)).toList();
		List<EvaluatedDocument> highDocs = evaluatedDocuments.stream()
			.filter(document -> document.score() >= upperThreshold)
			.toList();
		List<EvaluatedDocument> goodDocs = evaluatedDocuments.stream()
			.filter(document -> document.score() > lowerThreshold && document.score() < upperThreshold)
			.toList();
		List<EvaluatedDocument> badDocs = evaluatedDocuments.stream()
			.filter(document -> document.score() <= lowerThreshold)
			.toList();

		OverallRetrievalDecision.Decision decision = decide(highDocs, goodDocs);
		logger.info(
			"Retrieval set evaluation total={} high(>=U:{})={} good(L,U:{}-{})={} bad(<=L:{})={} decision={}",
			evaluatedDocuments.size(), upperThreshold, highDocs.size(), lowerThreshold, upperThreshold, goodDocs.size(),
			lowerThreshold, badDocs.size(), decision);
		return new OverallRetrievalDecision(decision, goodDocs, badDocs, highDocs);
	}

	private OverallRetrievalDecision.Decision decide(List<EvaluatedDocument> highDocs, List<EvaluatedDocument> goodDocs) {
		if (!highDocs.isEmpty()) {
			return OverallRetrievalDecision.Decision.CORRECT;
		}
		if (goodDocs.isEmpty()) {
			return OverallRetrievalDecision.Decision.INCORRECT;
		}
		return OverallRetrievalDecision.Decision.AMBIGUOUS;
	}

	public EvaluatedDocument evaluate(String query, String chunk) {
		if (!StringUtils.hasText(query)) {
			throw new IllegalArgumentException("query must not be blank");
		}
		if (!StringUtils.hasText(chunk)) {
			throw new IllegalArgumentException("chunk must not be blank");
		}
		try {
			String response = llmService.generate(buildEvaluationPrompt(query, chunk));
			double score = parseScore(response);
			EvaluatedDocument.Classification thresholdClassification = classifyByThreshold(score);
			logger.info("Retrieval evaluation score={} classification={} for query='{}'", score, thresholdClassification,
				query);
			return new EvaluatedDocument(chunk, score, thresholdClassification);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to evaluate retrieval relevance", ex);
		}
	}

	private String buildEvaluationPrompt(String query, String chunk) {
		return """
				Score the relevance of the retrieved context chunk for the user query.
				Return exactly one line in this format:
				score: <number between 0 and 1>
				
				Query:
				%s
				
				Chunk:
				%s
				""".formatted(query, chunk);
	}

	private double parseScore(String response) {
		Matcher matcher = SCORE_PATTERN.matcher(response == null ? "" : response);
		if (!matcher.find()) {
			throw new IllegalStateException("Could not parse score from evaluator response: " + response);
		}
		double score = Double.parseDouble(matcher.group(1));
		if (score < 0.0d || score > 1.0d) {
			throw new IllegalStateException("Evaluator score out of range: " + score);
		}
		return score;
	}

	private EvaluatedDocument.Classification classifyByThreshold(double score) {
		if (score >= upperThreshold) {
			return EvaluatedDocument.Classification.CORRECT;
		}
		if (score <= lowerThreshold) {
			return EvaluatedDocument.Classification.INCORRECT;
		}
		return EvaluatedDocument.Classification.AMBIGUOUS;
	}

}
