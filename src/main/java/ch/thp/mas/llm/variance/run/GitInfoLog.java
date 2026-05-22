package ch.thp.mas.llm.variance.run;

public record GitInfoLog(
        String commitSha,
        String branch,
        boolean dirty,
        String probeError
) {
}
