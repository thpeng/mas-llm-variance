package ch.thp.mas.llm.variance.analyze.semantic;


import ch.thp.mas.llm.variance.analyze.TextTokenizer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AnswerChunker {

    private final TextTokenizer tokenizer;

    public AnswerChunker(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public List<String> chunk(String answer, ChunkConfig config, int maxTokens) {
        List<String> blocks = paragraphBlocks(answer);
        if (blocks.isEmpty()) {
            return List.of(answer == null ? "" : answer.trim());
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        int target = Math.min(config.targetTokens(), maxTokens);

        for (String block : blocks) {
            List<String> blockParts = splitBlock(block, maxTokens);
            for (String part : blockParts) {
                int partTokens = tokenizer.tokenize(part).size();
                if (!current.isEmpty() && currentTokens + partTokens > target) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                    currentTokens = 0;
                }
                if (!current.isEmpty()) {
                    current.append("\n\n");
                }
                current.append(part);
                currentTokens += partTokens;
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private List<String> paragraphBlocks(String answer) {
        if (answer == null || answer.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(answer.trim().split("\\R\\s*\\R"))
                .map(String::trim)
                .filter(block -> !block.isBlank())
                .toList();
    }

    private List<String> splitBlock(String block, int maxTokens) {
        List<String> tokens = tokenizer.tokenize(block);
        if (tokens.size() <= maxTokens) {
            return List.of(block);
        }

        List<String> words = java.util.Arrays.stream(block.split("\\s+"))
                .filter(word -> !word.isBlank())
                .toList();
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        for (String word : words) {
            int wordTokens = tokenizer.tokenize(word).size();
            if (!current.isEmpty() && currentTokens + wordTokens > maxTokens) {
                parts.add(current.toString().trim());
                current.setLength(0);
                currentTokens = 0;
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
            currentTokens += wordTokens;
        }
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        return parts;
    }
}
