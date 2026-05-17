package ch.thp.mas.llm.variance.run;

public record RunConfigLog(
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        String reasoning
) {
}
