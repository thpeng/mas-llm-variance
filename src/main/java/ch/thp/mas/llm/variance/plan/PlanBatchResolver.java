package ch.thp.mas.llm.variance.plan;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class PlanBatchResolver {

    private final PlanLoader planLoader;
    private final PlanResolver planResolver;

    public PlanBatchResolver(PlanLoader planLoader, PlanResolver planResolver) {
        this.planLoader = planLoader;
        this.planResolver = planResolver;
    }

    public List<ResolvedPlan> resolve(ApplicationArguments appArgs) {
        if (appArgs.containsOption("plan") || appArgs.containsOption("plans")) {
            throw new PlanException("The plan command was renamed. Use --run=<plans|plans/subfolder|plan>.");
        }
        if (!appArgs.containsOption("run")) {
            throw new PlanException("Missing run selection. Use --run=<plans|plans/subfolder|plan>.");
        }

        List<LoadedPlan> loadedPlans = planLoader.loadSelection(optionValue(appArgs, "run"));

        return loadedPlans.stream()
                .map(plan -> planResolver.resolve(plan, appArgs))
                .toList();
    }

    private static String optionValue(ApplicationArguments appArgs, String name) {
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }
}
