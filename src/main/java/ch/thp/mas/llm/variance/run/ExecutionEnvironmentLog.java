package ch.thp.mas.llm.variance.run;

public record ExecutionEnvironmentLog(
        GitInfoLog git,
        RuntimeInfoLog runtime,
        GpuEnvironmentLog gpu
) {
}
