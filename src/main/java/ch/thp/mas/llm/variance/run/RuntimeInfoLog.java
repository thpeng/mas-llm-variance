package ch.thp.mas.llm.variance.run;

public record RuntimeInfoLog(
        String javaVersion,
        String javaVendor,
        String osName,
        String osVersion,
        String osArch
) {
}
