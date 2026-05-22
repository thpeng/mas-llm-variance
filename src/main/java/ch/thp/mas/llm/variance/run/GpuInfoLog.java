package ch.thp.mas.llm.variance.run;

public record GpuInfoLog(
        String name,
        String driverVersion,
        Integer memoryTotalMiB
) {
}
