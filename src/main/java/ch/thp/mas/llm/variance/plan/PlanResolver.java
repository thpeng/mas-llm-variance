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
                : ((YamlPlan) loadedPlan.plan()).getRun().getSeed();
        if (inferenceProvider == InferenceProvider.LMSTUDIO && seedSetting != null && !seedSetting.isBlank()) {
            throw new PlanException("LM Studio does not support seed configuration in plan: " + loadedPlan.filename());
        }
        Long seed = seedSetting == null || seedSetting.isBlank() ? null : parseSeed(seedSetting);
        String reasoningValue = optionValue(appArgs, "reasoning") != null
                ? optionValue(appArgs, "reasoning")
                : plan.getReasoning();
        Reasoning reasoning = parseReasoning(reasoningValue);
        String reasoningProviderValue = optionValue(appArgs, "reasoningProviderValue") != null
                ? optionValue(appArgs, "reasoningProviderValue")
                : plan.getReasoningProviderValue();
        if (isQwenLmStudioReasoningOn(inferenceProvider, model, reasoningValue)) {
            reasoningProviderValue = "on";
        }
        boolean sendReasoning = optionValue(appArgs, "sendReasoning") != null
                ? parseBoolean(optionValue(appArgs, "sendReasoning"), "sendReasoning")
                : sendReasoning(plan, reasoningValue, reasoningProviderValue);
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
                seedSetting == null || seedSetting.isBlank() ? null : normalizedSeedSetting(seedSetting),
                reasoning,
                sendReasoning,
                blankToNull(reasoningProviderValue),
                plan.getLoad(),
                modelVersion,
                loadedPlan.filename()
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
    }

    private static boolean sendReasoning(Plan plan, String reasoningValue, String reasoningProviderValue) {
        Boolean sendReasoning = plan.getSendReasoning();
        if (sendReasoning != null) {
            return sendReasoning;
        }
        return !isBlank(reasoningValue) || !isBlank(reasoningProviderValue);
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
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("on".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Reasoning.parse(value);
        } catch (IllegalArgumentException e) {
            throw new PlanException(e.getMessage(), e);
        }
    }

    private static boolean isQwenLmStudioReasoningOn(
            InferenceProvider inferenceProvider,
            String model,
            String reasoningValue
    ) {
        if (!"on".equalsIgnoreCase(reasoningValue == null ? null : reasoningValue.trim())) {
            return false;
        }
        if (inferenceProvider == InferenceProvider.LMSTUDIO && model != null
                && model.toLowerCase(Locale.ROOT).contains("qwen")) {
            return true;
        }
        throw new PlanException("Reasoning 'on' is only supported for Qwen via LM Studio.");
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
