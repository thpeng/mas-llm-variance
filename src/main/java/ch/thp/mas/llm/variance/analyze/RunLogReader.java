package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.run.RunLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RunLogReader {

    private static final Path DEFAULT_RUNS_DIRECTORY = Path.of("src", "main", "resources", "runs");
    private static final String RUN_ROOT_NAME = "runs";

    private final Path runsDirectory;
    private final ObjectMapper objectMapper;

    public RunLogReader() {
        this(DEFAULT_RUNS_DIRECTORY, defaultObjectMapper());
    }

    RunLogReader(Path runsDirectory, ObjectMapper objectMapper) {
        this.runsDirectory = runsDirectory.normalize();
        this.objectMapper = objectMapper;
    }

    public NamedRunLog read(String selection) {
        Path path = resolveFileSelection(selection);
        return read(path);
    }

    public List<NamedRunLog> readSelection(String selection) {
        Path path = resolveSelection(selection);
        if (Files.isDirectory(path)) {
            return readDirectory(path);
        }
        return List.of(read(path));
    }

    public List<NamedRunLog> readAll() {
        return readDirectory(runsDirectory);
    }

    private List<NamedRunLog> readDirectory(Path directory) {
        List<Path> files = runFiles(directory);
        if (files.isEmpty()) {
            throw new AnalysisException("No run logs found in: " + displayPath(directory));
        }
        ensureUniqueRunFilenames(files);
        return files.stream()
                .map(this::read)
                .toList();
    }

    private NamedRunLog read(Path path) {
        String filename = relativeName(path);
        try {
            return new NamedRunLog(filename, objectMapper.readValue(path.toFile(), RunLog.class));
        } catch (IOException e) {
            throw new AnalysisException("Could not read run log: " + filename, e);
        }
    }

    private Path resolveFileSelection(String selection) {
        Path path = resolveSelection(selection);
        if (Files.isDirectory(path)) {
            throw new AnalysisException("Run selection points to a folder, not a single run log: " + selection);
        }
        return path;
    }

    private Path resolveSelection(String selection) {
        String normalized = normalizeSelection(selection);
        Path direct = directPath(normalized);
        if (Files.exists(direct)) {
            ensureInsideRunsDirectory(direct);
            return direct.normalize();
        }
        if (isRootedSelection(normalized)) {
            throw new AnalysisException("Run selection not found: " + selection);
        }

        List<Path> fileMatches = matchingRunFiles(normalized);
        if (fileMatches.size() > 1) {
            throw new AnalysisException("Ambiguous run selection '" + selection + "': "
                    + fileMatches.stream().map(this::relativeName).sorted().toList());
        }
        if (fileMatches.size() == 1) {
            return fileMatches.getFirst();
        }

        List<Path> folderMatches = matchingFolders(normalized);
        if (folderMatches.size() > 1) {
            throw new AnalysisException("Ambiguous run folder selection '" + selection + "': "
                    + folderMatches.stream().map(this::relativeName).sorted().toList());
        }
        if (folderMatches.size() == 1) {
            return folderMatches.getFirst();
        }

        throw new AnalysisException("Run selection not found: " + selection);
    }

    private List<Path> matchingRunFiles(String selection) {
        List<String> candidateNames = candidateFileNames(selection);
        return runFiles(runsDirectory).stream()
                .filter(path -> candidateNames.contains(path.getFileName().toString())
                        || selection.equals(removeJsonSuffix(path.getFileName().toString())))
                .toList();
    }

    private List<Path> matchingFolders(String selection) {
        try (var stream = Files.walk(runsDirectory)) {
            return stream
                    .filter(path -> !path.equals(runsDirectory))
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals(selection))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new AnalysisException("Could not discover run folders under: " + runsDirectory, e);
        }
    }

    private List<Path> runFiles(Path directory) {
        if (!Files.exists(directory)) {
            throw new AnalysisException("Runs directory does not exist: " + displayPath(directory));
        }
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(this::relativeName))
                    .toList();
        } catch (IOException e) {
            throw new AnalysisException("Could not list run logs in: " + displayPath(directory), e);
        }
    }

    private void ensureUniqueRunFilenames(List<Path> files) {
        Map<String, List<Path>> grouped = files.stream()
                .collect(Collectors.groupingBy(path -> path.getFileName().toString()));
        List<String> duplicates = grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + "=" + entry.getValue().stream().map(this::relativeName).sorted().toList())
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new AnalysisException("Duplicate run log file names found: " + duplicates);
        }
    }

    private Path directPath(String selection) {
        Path path = Path.of(selection);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        String relative = stripRootPrefix(selection);
        return runsDirectory.resolve(relative).normalize();
    }

    private String relativeName(Path path) {
        return runsDirectory.relativize(path.normalize()).toString().replace('\\', '/');
    }

    private String displayPath(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private void ensureInsideRunsDirectory(Path path) {
        if (!path.normalize().startsWith(runsDirectory)) {
            throw new AnalysisException("Run selection must stay under " + RUN_ROOT_NAME + ": " + path);
        }
    }

    private static List<String> candidateFileNames(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json")) {
            return List.of(name);
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(name + ".json");
        return candidates;
    }

    private static String removeJsonSuffix(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".json")
                ? filename.substring(0, filename.length() - ".json".length())
                : filename;
    }

    private static String normalizeSelection(String selection) {
        if (selection == null || selection.isBlank()) {
            throw new AnalysisException("--analyze must specify a run log file or folder.");
        }
        String normalized = selection.trim().replace('\\', '/');
        if ("ALL".equalsIgnoreCase(normalized)) {
            return RUN_ROOT_NAME;
        }
        if (normalized.contains("..")) {
            throw new AnalysisException("Invalid run selection: " + selection);
        }
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private static String stripRootPrefix(String selection) {
        if (RUN_ROOT_NAME.equals(selection)) {
            return "";
        }
        String prefix = RUN_ROOT_NAME + "/";
        return selection.startsWith(prefix) ? selection.substring(prefix.length()) : selection;
    }

    private static boolean isRootedSelection(String selection) {
        return RUN_ROOT_NAME.equals(selection) || selection.startsWith(RUN_ROOT_NAME + "/") || Path.of(selection).isAbsolute();
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
