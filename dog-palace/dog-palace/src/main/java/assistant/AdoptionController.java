package assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
class AdoptionController {

    private final ChatClient ai;
    private final InMemoryChatMemoryRepository memoryRepository;

    AdoptionController(ChatClient.Builder ai, VectorStore vectorStore) {
        var system = """
                You are an AI-powered assistant to help people adopt a dog from the adoption
                agency named Dog Palace with locations in Antwerp, Seoul, Tokyo, Singapore, Paris,
                Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available
                will be presented below. If there is no information, then return a polite response suggesting we
                don't have any dogs available.
                """;

        this.memoryRepository = new InMemoryChatMemoryRepository();

        this.ai = ai
                .defaultSystem(system)
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    @GetMapping("/{user}/dogs/assistant")
    String inquire(@PathVariable String user, @RequestParam String question) {

        var memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(memoryRepository)
                .maxMessages(10)
                .build();

        var memoryAdvisor = MessageChatMemoryAdvisor.builder(memory).build();

        return this.ai
                .prompt()
                .user(question)
                .advisors(a -> a
                        .advisors(memoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, user)
                )
                .call()
                .content();
    }
}