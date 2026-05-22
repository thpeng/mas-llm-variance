package ch.thp.mas.llm.variance.run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ExecutionEnvironmentCollector {

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(5);

    private final Path workingDirectory;

    public ExecutionEnvironmentCollector() {
        this(Path.of("").toAbsolutePath());
    }

    ExecutionEnvironmentCollector(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public ExecutionEnvironmentLog snapshot() {
        return new ExecutionEnvironmentLog(gitInfo(), runtimeInfo(), gpuInfo());
    }

    public static ExecutionEnvironmentCollector noop() {
        return new NoopExecutionEnvironmentCollector();
    }

    private GitInfoLog gitInfo() {
        String commitSha = null;
        String branch = null;
        boolean dirty = false;
        List<String> errors = new ArrayList<>();

        CommandResult commitResult = runCommand("git", "rev-parse", "HEAD");
        if (commitResult.success()) {
            commitSha = commitResult.stdout().trim();
        } else {
            errors.add("commitSha: " + commitResult.message());
        }

        CommandResult branchResult = runCommand("git", "rev-parse", "--abbrev-ref", "HEAD");
        if (branchResult.success()) {
            branch = branchResult.stdout().trim();
        } else {
            errors.add("branch: " + branchResult.message());
        }

        CommandResult dirtyResult = runCommand("git", "status", "--porcelain");
        if (dirtyResult.success()) {
            dirty = !dirtyResult.stdout().isBlank();
        } else {
            errors.add("dirty: " + dirtyResult.message());
        }

        return new GitInfoLog(commitSha, branch, dirty, errors.isEmpty() ? null : String.join("; ", errors));
    }

    private RuntimeInfoLog runtimeInfo() {
        return new RuntimeInfoLog(
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch")
        );
    }

    private GpuEnvironmentLog gpuInfo() {
        CommandResult gpuResult = runCommand(
                "nvidia-smi",
                "--query-gpu=name,driver_version,memory.total",
                "--format=csv,noheader,nounits"
        );
        if (!gpuResult.success()) {
            return new GpuEnvironmentLog(List.of(), null, gpuResult.message());
        }

        List<GpuInfoLog> gpus = gpuResult.stdout().lines()
                .filter(line -> !line.isBlank())
                .map(this::parseGpuLine)
                .toList();

        String cudaVersion = null;
        String probeError = null;
        CommandResult cudaResult = runCommand("nvidia-smi");
        if (cudaResult.success()) {
            cudaVersion = extractCudaVersion(cudaResult.stdout());
        } else {
            probeError = "cuda: " + cudaResult.message();
        }
        return new GpuEnvironmentLog(gpus, cudaVersion, probeError);
    }

    private GpuInfoLog parseGpuLine(String line) {
        String[] parts = line.split(",", 3);
        String name = part(parts, 0);
        String driverVersion = part(parts, 1);
        Integer memoryTotalMiB = parseInteger(part(parts, 2));
        return new GpuInfoLog(name, driverVersion, memoryTotalMiB);
    }

    private static String extractCudaVersion(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("CUDA Version:\\s*([0-9.]+)")
                .matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private CommandResult runCommand(String... command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(false)
                    .start();
            if (!process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return CommandResult.failure("timeout");
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() == 0) {
                return CommandResult.success(stdout);
            }
            return CommandResult.failure(stderr.isBlank() ? stdout : stderr);
        } catch (IOException e) {
            return CommandResult.failure(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommandResult.failure("interrupted");
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String part(String[] parts, int index) {
        if (index >= parts.length) {
            return null;
        }
        String value = parts[index].trim();
        return value.isEmpty() ? null : value;
    }

    private static Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record CommandResult(boolean success, String stdout, String message) {

        static CommandResult success(String stdout) {
            return new CommandResult(true, stdout, null);
        }

        static CommandResult failure(String message) {
            return new CommandResult(false, "", message == null || message.isBlank() ? "unknown error" : message.trim());
        }
    }

    private static final class NoopExecutionEnvironmentCollector extends ExecutionEnvironmentCollector {

        private NoopExecutionEnvironmentCollector() {
            super(Path.of(""));
        }

        @Override
        public ExecutionEnvironmentLog snapshot() {
            return null;
        }
    }
}
