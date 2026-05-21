package ch.thp.mas.llm.variance.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum Reasoning {
    OFF("off"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    XHIGH("xhigh");

    private final String value;

    Reasoning(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Reasoning parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing reasoning");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "off" -> OFF;
            case "low" -> LOW;
            case "medium" -> MEDIUM;
            case "high" -> HIGH;
            case "xhigh" -> XHIGH;
            default -> throw new IllegalArgumentException("Unknown reasoning: " + value
                    + ". Supported values are: off, low, medium, high, xhigh.");
        };
    }

    /**
     * LM Studio mapping: off->off, low->low, medium->medium, high->high. xhigh is not supported.
     */
    public String lmStudioValue() {
        return switch (this) {
            case OFF, LOW, MEDIUM, HIGH -> value;
            case XHIGH -> throw unsupported("LM Studio");
        };
    }

    /**
     * Gemini mapping: off->minimal, low->low, medium->medium, high->high. xhigh is not supported.
     */
    public String geminiThinkingLevel() {
        return switch (this) {
            case OFF -> "minimal";
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
            case XHIGH -> throw unsupported("Gemini");
        };
    }

    /**
     * OpenAI mapping: off->none, low->low, medium->medium, high->high, xhigh->xhigh.
     */
    public String openAiReasoningEffort() {
        return switch (this) {
            case OFF -> "none";
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
            case XHIGH -> "xhigh";
        };
    }

    /**
     * Anthropic effort mapping with adaptive thinking:
     * low->low, medium->medium, high->high. off disables thinking and xhigh is not supported.
     */
    public String anthropicEffort() {
        return switch (this) {
            case OFF -> throw new IllegalArgumentException("Reasoning 'off' disables Anthropic thinking and has no effort value.");
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
            case XHIGH -> throw unsupported("Anthropic");
        };
    }

    private IllegalArgumentException unsupported(String provider) {
        return new IllegalArgumentException("Reasoning '" + value + "' is not supported for " + provider + ".");
    }
}
