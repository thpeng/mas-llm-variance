package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
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
public class AnalysisResultReader {

    private static final Path DEFAULT_ANALYSIS_DIRECTORY = Path.of("src", "main", "resources", "analysis");
    private static final String ANALYSIS_ROOT_NAME = "analysis";

    private final Path analysisDirectory;
    private final ObjectMapper objectMapper;

    public AnalysisResultReader() {
        this(DEFAULT_ANALYSIS_DIRECTORY, defaultObjectMapper());
    }

    AnalysisResultReader(Path analysisDirectory, ObjectMapper objectMapper) {
        this.analysisDirectory = analysisDirectory.normalize();
        this.objectMapper = objectMapper;
    }

    public List<NamedAnalysisResult> readSelection(String selection) {
        Path path = resolveSelection(selection);
        if (Files.isDirectory(path)) {
            return readDirectory(path);
        }
        return List.of(read(path));
    }

    private List<NamedAnalysisResult> readDirectory(Path directory) {
        List<Path> files = analysisFiles(directory);
        if (files.isEmpty()) {
            throw new MetaAnalysisException("No analysis files found in: " + displayPath(directory));
        }
        ensureUniqueAnalysisFilenames(files);
        return files.stream()
                .map(this::read)
                .toList();
    }

    private NamedAnalysisResult read(Path path) {
        String filename = relativeName(path);
        try {
            return new NamedAnalysisResult(filename, objectMapper.readValue(path.toFile(), AnalysisResult.class));
        } catch (IOException e) {
            throw new MetaAnalysisException("Could not read analysis file: " + filename, e);
        }
    }

    private Path resolveSelection(String selection) {
        String normalized = normalizeSelection(selection);
        Path direct = directPath(normalized);
        if (Files.exists(direct)) {
            ensureInsideAnalysisDirectory(direct);
            return direct.normalize();
        }
        if (isRootedSelection(normalized)) {
            throw new MetaAnalysisException("Analysis selection not found: " + selection);
        }

        List<Path> fileMatches = matchingAnalysisFiles(normalized);
        if (fileMatches.size() > 1) {
            throw new MetaAnalysisException("Ambiguous analysis selection '" + selection + "': "
                    + fileMatches.stream().map(this::relativeName).sorted().toList());
        }
        if (fileMatches.size() == 1) {
            return fileMatches.getFirst();
        }

        List<Path> folderMatches = matchingFolders(normalized);
        if (folderMatches.size() > 1) {
            throw new MetaAnalysisException("Ambiguous analysis folder selection '" + selection + "': "
                    + folderMatches.stream().map(this::relativeName).sorted().toList());
        }
        if (folderMatches.size() == 1) {
            return folderMatches.getFirst();
        }

        throw new MetaAnalysisException("Analysis selection not found: " + selection);
    }

    private List<Path> matchingAnalysisFiles(String selection) {
        List<String> candidateNames = candidateFileNames(selection);
        return analysisFiles(analysisDirectory).stream()
                .filter(path -> candidateNames.contains(path.getFileName().toString())
                        || selection.equals(removeJsonSuffix(path.getFileName().toString())))
                .toList();
    }

    private List<Path> matchingFolders(String selection) {
        try (var stream = Files.walk(analysisDirectory)) {
            return stream
                    .filter(path -> !path.equals(analysisDirectory))
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals(selection))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new MetaAnalysisException("Could not discover analysis folders under: " + analysisDirectory, e);
        }
    }

    private List<Path> analysisFiles(Path directory) {
        if (!Files.exists(directory)) {
            throw new MetaAnalysisException("Analysis directory does not exist: " + displayPath(directory));
        }
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(this::relativeName))
                    .toList();
        } catch (IOException e) {
            throw new MetaAnalysisException("Could not list analysis files in: " + displayPath(directory), e);
        }
    }

    private void ensureUniqueAnalysisFilenames(List<Path> files) {
        Map<String, List<Path>> grouped = files.stream()
                .collect(Collectors.groupingBy(path -> path.getFileName().toString()));
        List<String> duplicates = grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + "=" + entry.getValue().stream().map(this::relativeName).sorted().toList())
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new MetaAnalysisException("Duplicate analysis file names found: " + duplicates);
        }
    }

    private Path directPath(String selection) {
        Path path = Path.of(selection);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        String relative = stripRootPrefix(selection);
        return analysisDirectory.resolve(relative).normalize();
    }

    private String relativeName(Path path) {
        return analysisDirectory.relativize(path.normalize()).toString().replace('\\', '/');
    }

    private String displayPath(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private void ensureInsideAnalysisDirectory(Path path) {
        if (!path.normalize().startsWith(analysisDirectory)) {
            throw new MetaAnalysisException("Analysis selection must stay under " + ANALYSIS_ROOT_NAME + ": " + path);
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
            throw new MetaAnalysisException("--metanalysis must specify an analysis file or folder.");
        }
        String normalized = selection.trim().replace('\\', '/');
        if ("ALL".equalsIgnoreCase(normalized)) {
            return ANALYSIS_ROOT_NAME;
        }
        if (normalized.contains("..")) {
            throw new MetaAnalysisException("Invalid analysis selection: " + selection);
        }
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private static String stripRootPrefix(String selection) {
        if (ANALYSIS_ROOT_NAME.equals(selection)) {
            return "";
        }
        String prefix = ANALYSIS_ROOT_NAME + "/";
        return selection.startsWith(prefix) ? selection.substring(prefix.length()) : selection;
    }

    private static boolean isRootedSelection(String selection) {
        return ANALYSIS_ROOT_NAME.equals(selection)
                || selection.startsWith(ANALYSIS_ROOT_NAME + "/")
                || Path.of(selection).isAbsolute();
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
