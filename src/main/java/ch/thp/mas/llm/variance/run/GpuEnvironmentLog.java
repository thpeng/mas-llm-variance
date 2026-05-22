package ch.thp.mas.llm.variance.run;

import java.util.List;

public record GpuEnvironmentLog(
        List<GpuInfoLog> nvidiaGpus,
        String cudaVersion,
        String probeError
) {

    public GpuEnvironmentLog {
        nvidiaGpus = nvidiaGpus == null ? List.of() : List.copyOf(nvidiaGpus);
    }
}
