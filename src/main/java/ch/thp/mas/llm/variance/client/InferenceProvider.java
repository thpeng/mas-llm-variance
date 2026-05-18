package ch.thp.mas.llm.variance.client;

public enum InferenceProvider {

    OPENAI {
        @Override
        public LlmClient createClient() {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("OPENAI_API_KEY environment variable is not set.");
            }
            return new OpenAiClient(apiKey);
        }

        @Override
        public String defaultModel() {
            return "gpt-5-mini-2025-08-07";
        }
    },

    ANTHROPIC {
        @Override
        public LlmClient createClient() {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is not set.");
            }
            return new AnthropicClient(apiKey);
        }

        @Override
        public String defaultModel() {
            return "claude-sonnet-4-5-20250929";
        }
    },

    GEMINI {
        @Override
        public LlmClient createClient() {
            String apiKey = System.getenv("GOOGLE_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("GOOGLE_API_KEY environment variable is not set.");
            }
            return new GeminiClient(apiKey);
        }

        @Override
        public String defaultModel() {
            return "gemini-3-flash";
        }
    },

    LMSTUDIO {
        @Override
        public LlmClient createClient() {
            String baseUrl = System.getenv("LMSTUDIO_BASE_URL");
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "http://127.0.0.1:10022";
            }
            return new LmStudioChatClient(baseUrl, System.getenv("LM_API_TOKEN"));
        }

        @Override
        public String defaultModel() {
            return "swiss-ai_Apertus-8B-Instruct-2509-GGUF";
        }
    };

    public abstract LlmClient createClient();

    public abstract String defaultModel();
}
