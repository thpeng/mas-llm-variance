package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class PlanResolver {

    private static final String RANDOM_SEED = "random";

    public ResolvedPlan resolve(LoadedPlan loadedPlan, ApplicationArguments appArgs) {
        Plan plan = loadedPlan.plan();
        requireRunBlock(loadedPlan);
        requireRunConfiguration(loadedPlan);
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
        String seedSetting = optionValue(appArgs, "seed") != null
                ? optionValue(appArgs, "seed")
                : seedSetting(loadedPlan);
        Long seed = parseSeed(seedSetting);
        String reasoningValue = optionValue(appArgs, "reasoning") != null
                ? optionValue(appArgs, "reasoning")
                : plan.getReasoning();
        Reasoning reasoning = parseReasoning(reasoningValue);
        boolean sendReasoning = optionValue(appArgs, "sendReasoning") != null
                ? parseBoolean(optionValue(appArgs, "sendReasoning"), "sendReasoning")
                : sendReasoning(loadedPlan);
        String reasoningProviderValue = optionValue(appArgs, "reasoningProviderValue") != null
                ? optionValue(appArgs, "reasoningProviderValue")
                : plan.getReasoningProviderValue();
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
                normalizedSeedSetting(seedSetting),
                reasoning,
                sendReasoning,
                blankToNull(reasoningProviderValue),
                plan.getLoad(),
                modelVersion
        );
    }

    private static void requireRunBlock(LoadedPlan loadedPlan) {
        if (((YamlPlan) loadedPlan.plan()).getRun() == null) {
            throw new PlanException("Missing run block in plan: " + loadedPlan.filename());
        }
    }

    private static void requireRunConfiguration(LoadedPlan loadedPlan) {
        YamlRunConfig run = ((YamlPlan) loadedPlan.plan()).getRun();
        requirePresent(run.getPrompt(), "run.prompt", loadedPlan);
        requirePresent(run.getIterations(), "run.iterations", loadedPlan);
        requirePresent(run.getTemperature(), "run.temperature", loadedPlan);
        requirePresent(run.getTopP(), "run.topP", loadedPlan);
        requirePresent(run.getTopK(), "run.topK", loadedPlan);
        requirePresent(run.getSeed(), "run.seed", loadedPlan);
        requirePresent(run.getReasoning(), "run.reasoning", loadedPlan);
    }

    private static String seedSetting(LoadedPlan loadedPlan) {
        String seed = ((YamlPlan) loadedPlan.plan()).getRun().getSeed();
        requirePresent(seed, "run.seed", loadedPlan);
        return seed;
    }

    private static boolean sendReasoning(LoadedPlan loadedPlan) {
        Boolean sendReasoning = ((YamlPlan) loadedPlan.plan()).getRun().getSendReasoning();
        return sendReasoning == null || sendReasoning;
    }

    private static Long parseSeed(String value) {
        if (RANDOM_SEED.equalsIgnoreCase(value.trim())) {
            return null;
        }
        return parseLong(value, "seed");
    }

    private static String normalizedSeedSetting(String value) {
        if (RANDOM_SEED.equalsIgnoreCase(value.trim())) {
            return "RANDOM";
        }
        return Long.toString(parseLong(value, "seed"));
    }

    private static void requirePresent(Object value, String name, LoadedPlan loadedPlan) {
        if (value == null) {
            throw new PlanException("Missing " + name + " in plan: " + loadedPlan.filename());
        }
        if (value instanceof String string && string.isBlank()) {
            throw new PlanException("Missing " + name + " in plan: " + loadedPlan.filename());
        }
    }

    private static InferenceProvider parseInferenceProvider(String value) {
        try {
            return InferenceProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PlanException("Unknown inferenceProvider: " + value, e);
        }
    }

    private static Reasoning parseReasoning(String value) {
        try {
            return Reasoning.parse(value);
        } catch (IllegalArgumentException e) {
            throw new PlanException(e.getMessage(), e);
        }
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

    private static boolean parseBoolean(String value, String name) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        throw new PlanException("Invalid boolean value for " + name + ": " + value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
