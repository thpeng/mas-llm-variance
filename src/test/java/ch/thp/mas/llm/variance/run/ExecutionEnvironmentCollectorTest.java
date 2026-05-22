package ch.thp.mas.llm.variance.run;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExecutionEnvironmentCollectorTest {

    @Test
    void extractsCudaVersionFromNvidiaSmiVersionOutput() {
        String output = """
                NVIDIA-SMI version  : 591.55
                NVML version        : 591.55
                DRIVER version      : 591.55
                CUDA Version        : 13.1
                """;

        assertThat(ExecutionEnvironmentCollector.extractCudaVersion(output)).isEqualTo("13.1");
    }

    @Test
    void extractsCudaVersionFromNvidiaSmiTableOutput() {
        String output = """
                | NVIDIA-SMI 591.55                 Driver Version: 591.55         CUDA Version: 13.1     |
                """;

        assertThat(ExecutionEnvironmentCollector.extractCudaVersion(output)).isEqualTo("13.1");
    }
}
