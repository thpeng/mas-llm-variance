package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class PlanResolver {

    public ResolvedPlan resolve(LoadedPlan loadedPlan, ApplicationArguments appArgs) {
        Plan plan = loadedPlan.plan();
        InferenceProvider inferenceProvider = optionValue(appArgs, "inferenceProvider") != null
                ? parseInferenceProvider(optionValue(appArgs, "inferenceProvider"))
                : plan.getInferenceProvider();

        String model = optionValue(appArgs, "model") != null
                ? optionValue(appArgs, "model")
                : plan.getModel();
        if (model == null || model.isBlank()) {
            model = inferenceProvider.defaultModel();
        }

        String prompt = optionValue(appArgs, "prompt") != null
                ? optionValue(appArgs, "prompt")
                : plan.getPrompt();
        if (prompt == null || prompt.isBlank()) {
            throw new PlanException("Missing prompt in plan: " + loadedPlan.filename());
        }

        int iterations = optionValue(appArgs, "iterations") != null
                ? parseInteger(optionValue(appArgs, "iterations"), "iterations")
                : plan.getIterations();
        if (iterations < 1) {
            throw new PlanException("iterations must be at least 1 in plan: " + loadedPlan.filename());
        }

        Double temperature = optionValue(appArgs, "temperature") != null
                ? parseDouble(optionValue(appArgs, "temperature"), "temperature")
                : plan.getTemperature();
        Double topP = optionValue(appArgs, "topP") != null
                ? parseDouble(optionValue(appArgs, "topP"), "topP")
                : plan.getTopP();
        Integer topK = optionValue(appArgs, "topK") != null
                ? parseInteger(optionValue(appArgs, "topK"), "topK")
                : plan.getTopK();
        Long seed = optionValue(appArgs, "seed") != null
                ? parseLong(optionValue(appArgs, "seed"), "seed")
                : plan.getSeed();
        String reasoning = optionValue(appArgs, "reasoning") != null
                ? optionValue(appArgs, "reasoning")
                : plan.getReasoning();
        reasoning = normalizeReasoning(reasoning);
        String modelVersion = optionValue(appArgs, "modelVersion");

        return new ResolvedPlan(
                loadedPlan.name(),
                inferenceProvider,
                model,
                prompt,
                iterations,
                temperature,
                topP,
                topK,
                seed,
                reasoning,
                plan.getLoad(),
                modelVersion
        );
    }

    private static InferenceProvider parseInferenceProvider(String value) {
        try {
            return InferenceProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PlanException("Unknown inferenceProvider: " + value, e);
        }
    }

    private static String normalizeReasoning(String value) {
        if (value == null || value.isBlank()) {
            return "off";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "off", "low", "medium", "high", "on" -> normalized;
            default -> throw new PlanException("Unknown reasoning: " + value);
        };
    }

    private static Double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new PlanException("Invalid numeric value for " + name + ": " + value, e);
        }
    }

    private static Integer parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new PlanException("Invalid integer value for " + name + ": " + value, e);
        }
    }

    private static Long parseLong(String value, String name) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new PlanException("Invalid integer value for " + name + ": " + value, e);
        }
    }

    private static String optionValue(ApplicationArguments appArgs, String name) {
        if (!appArgs.containsOption(name)) {
            return null;
        }
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }
}
