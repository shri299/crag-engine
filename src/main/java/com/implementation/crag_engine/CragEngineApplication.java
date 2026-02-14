package com.implementation.crag_engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.implementation.crag_engine.ingestion.DataIngestionService;
import com.implementation.crag_engine.ingestion.IngestionProperties;

@SpringBootApplication
@EnableConfigurationProperties(IngestionProperties.class)
public class CragEngineApplication {

	private static final Logger logger = LoggerFactory.getLogger(CragEngineApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CragEngineApplication.class, args);
	}

	@Bean
	CommandLineRunner ingestionRunner(DataIngestionService ingestionService, IngestionProperties properties) {
		return args -> {
			if (!properties.isAutoRun()) {
				logger.info("Skipping ingestion pipeline because ingestion.auto-run=false");
				return;
			}
			logger.info("Starting ingestion pipeline");
			ingestionService.ingest();
			logger.info("Ingestion pipeline finished");
		};
	}

}
