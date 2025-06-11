package dev.darel.dog_palace;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.nio.charset.Charset;
import java.util.List;


@ImportRuntimeHints(DogPalaceApplication.Hints.class)
@SpringBootApplication
public class DogPalaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DogPalaceApplication.class, args);
	}

	static final Resource DOGS_JSON_FILE = new ClassPathResource("/dogs.json");

	static class Hints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerResource(DOGS_JSON_FILE);
		}
	}

	@Bean
	ApplicationRunner mongoDbInitializer(MongoTemplate template,
										 VectorStore vectorStore,
										 @Value("${spring.ai.vectorstore.mongodb.collection-name}") String collectionName,
										 ObjectMapper objectMapper) {
		return args -> {

			if (template.collectionExists(collectionName) && template.estimatedCount(collectionName) > 0)
				return;

			var documentData = DOGS_JSON_FILE.getContentAsString(Charset.defaultCharset());
			var jsonNode = objectMapper.readTree(documentData);
			jsonNode.spliterator().forEachRemaining(jsonNode1 -> {
				var id = jsonNode1.get("id").intValue();
				var name = jsonNode1.get("name").textValue();
				var description = jsonNode1.get("description").textValue();
				var dogument = new Document("id: %s, name: %s, description: %s".formatted(
						id, name, description
				));
				vectorStore.add(List.of(dogument));
			});
		};
	}
}
