package ch.thp.mas.llm.variance.plan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

@Component
public class PlanLoader {

    private static final Path DEFAULT_PLANS_DIRECTORY = Path.of("src", "main", "resources", "plans");
    private static final String PLAN_ROOT_NAME = "plans";
    private static final Pattern PLAN_FILE_PATTERN = Pattern.compile("^(\\d{4})-.+\\.ya?ml$");
    private static final List<String> SUFFIXES = List.of(".yml", ".yaml");

    private final Path plansDirectory;

    public PlanLoader() {
        this(DEFAULT_PLANS_DIRECTORY);
    }

    PlanLoader(Path plansDirectory) {
        this.plansDirectory = plansDirectory.normalize();
    }

    public LoadedPlan load(String selection) {
        Path path = resolveFileSelection(selection);
        return read(path, relativeName(path));
    }

    public List<LoadedPlan> loadSelection(String selection) {
        Path path = resolveSelection(selection);
        if (Files.isDirectory(path)) {
            return loadDirectory(path);
        }
        return List.of(read(path, relativeName(path)));
    }

    public List<LoadedPlan> loadAll() {
        return loadDirectory(plansDirectory);
    }

    public List<String> discoverPlanNames() {
        List<Path> files = planFiles(plansDirectory);
        ensureUniquePlanNames(files);
        return files.stream()
                .map(path -> removeYamlSuffix(path.getFileName().toString()))
                .sorted(naturalPlanNameComparator())
                .toList();
    }

    public static Comparator<String> naturalPlanNameComparator() {
        return Comparator
                .comparingInt(PlanLoader::planNumber)
                .thenComparing(String::compareTo);
    }

    static String removeYamlSuffix(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".yaml")) {
            return filename.substring(0, filename.length() - ".yaml".length());
        }
        if (lower.endsWith(".yml")) {
            return filename.substring(0, filename.length() - ".yml".length());
        }
        return filename;
    }

    private List<LoadedPlan> loadDirectory(Path directory) {
        List<Path> files = planFiles(directory);
        if (files.isEmpty()) {
            throw new PlanException("No plan files found in: " + displayPath(directory));
        }
        ensureUniquePlanNames(files);
        return files.stream()
                .map(path -> read(path, relativeName(path)))
                .toList();
    }

    private LoadedPlan read(Path path, String filename) {
        validatePlanFileName(path.getFileName().toString());
        try (InputStream inputStream = Files.newInputStream(path)) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(YamlPlan.class, loaderOptions));
            YamlPlan plan = yaml.load(inputStream);
            if (plan == null) {
                throw new PlanException("Plan file is empty: " + filename);
            }
            return new LoadedPlan(removeYamlSuffix(path.getFileName().toString()), filename, plan);
        } catch (YAMLException e) {
            throw new PlanException("Invalid YAML in plan file: " + filename, e);
        } catch (IOException e) {
            throw new PlanException("Could not read plan file: " + filename, e);
        }
    }

    private Path resolveFileSelection(String selection) {
        Path path = resolveSelection(selection);
        if (Files.isDirectory(path)) {
            throw new PlanException("Plan selection points to a folder, not a single plan: " + selection);
        }
        return path;
    }

    private Path resolveSelection(String selection) {
        String normalized = normalizeSelection(selection);
        Path direct = directPath(normalized);
        if (Files.exists(direct)) {
            ensureInsidePlansDirectory(direct);
            return direct.normalize();
        }
        if (isRootedSelection(normalized)) {
            throw new PlanException("Plan selection not found: " + selection);
        }

        List<Path> fileMatches = matchingPlanFiles(normalized);
        if (fileMatches.size() > 1) {
            throw new PlanException("Ambiguous plan selection '" + selection + "': "
                    + fileMatches.stream().map(this::relativeName).sorted().toList());
        }
        if (fileMatches.size() == 1) {
            return fileMatches.getFirst();
        }

        List<Path> folderMatches = matchingFolders(normalized);
        if (folderMatches.size() > 1) {
            throw new PlanException("Ambiguous plan folder selection '" + selection + "': "
                    + folderMatches.stream().map(this::relativeName).sorted().toList());
        }
        if (folderMatches.size() == 1) {
            return folderMatches.getFirst();
        }

        throw new PlanException("Plan selection not found: " + selection);
    }

    private List<Path> matchingPlanFiles(String selection) {
        List<String> candidateNames = candidateFileNames(selection);
        return planFiles(plansDirectory).stream()
                .filter(path -> candidateNames.contains(path.getFileName().toString())
                        || selection.equals(removeYamlSuffix(path.getFileName().toString())))
                .toList();
    }

    private List<Path> matchingFolders(String selection) {
        try (var stream = Files.walk(plansDirectory)) {
            return stream
                    .filter(path -> !path.equals(plansDirectory))
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals(selection))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new PlanException("Could not discover plan folders under: " + plansDirectory, e);
        }
    }

    private List<Path> planFiles(Path directory) {
        if (!Files.exists(directory)) {
            throw new PlanException("Plans directory does not exist: " + displayPath(directory));
        }
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> isYaml(path.getFileName().toString()))
                    .peek(path -> validatePlanFileName(path.getFileName().toString()))
                    .sorted(Comparator
                            .comparingInt((Path path) -> planNumber(removeYamlSuffix(path.getFileName().toString())))
                            .thenComparing(path -> relativeName(path)))
                    .toList();
        } catch (IOException e) {
            throw new PlanException("Could not discover plans under: " + displayPath(directory), e);
        }
    }

    private void ensureUniquePlanNames(List<Path> files) {
        Map<String, List<Path>> grouped = files.stream()
                .collect(Collectors.groupingBy(path -> removeYamlSuffix(path.getFileName().toString())));
        List<String> duplicates = grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + "=" + entry.getValue().stream().map(this::relativeName).sorted().toList())
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new PlanException("Duplicate plan file names found: " + duplicates);
        }
    }

    private Path directPath(String selection) {
        Path path = Path.of(selection);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        String relative = stripRootPrefix(selection);
        return plansDirectory.resolve(relative).normalize();
    }

    private String relativeName(Path path) {
        return plansDirectory.relativize(path.normalize()).toString().replace('\\', '/');
    }

    private String displayPath(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private void ensureInsidePlansDirectory(Path path) {
        if (!path.normalize().startsWith(plansDirectory)) {
            throw new PlanException("Plan selection must stay under " + PLAN_ROOT_NAME + ": " + path);
        }
    }

    private static List<String> candidateFileNames(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (isYaml(lower)) {
            return List.of(name);
        }

        List<String> candidates = new ArrayList<>();
        for (String suffix : SUFFIXES) {
            candidates.add(name + suffix);
        }
        return candidates;
    }

    private static String normalizeSelection(String selection) {
        if (selection == null || selection.isBlank()) {
            throw new PlanException("--run must specify a plan file or folder.");
        }

        String normalized = selection.trim().replace('\\', '/');
        if (normalized.contains("..")) {
            throw new PlanException("Invalid plan selection: " + selection);
        }
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private static String stripRootPrefix(String selection) {
        if (PLAN_ROOT_NAME.equals(selection)) {
            return "";
        }
        String prefix = PLAN_ROOT_NAME + "/";
        return selection.startsWith(prefix) ? selection.substring(prefix.length()) : selection;
    }

    private static boolean isRootedSelection(String selection) {
        return PLAN_ROOT_NAME.equals(selection) || selection.startsWith(PLAN_ROOT_NAME + "/") || Path.of(selection).isAbsolute();
    }

    private static boolean isYaml(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yml") || lower.endsWith(".yaml");
    }

    private static void validatePlanFileName(String filename) {
        if (!PLAN_FILE_PATTERN.matcher(filename).matches()) {
            throw new PlanException("Plan file name must start with a four digit number: " + filename);
        }
    }

    private static int planNumber(String name) {
        String filename = candidateFileNames(normalizePlanName(name)).getFirst();
        Matcher matcher = PLAN_FILE_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            throw new PlanException("Plan file name must start with a four digit number: " + name);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String normalizePlanName(String name) {
        if (name == null || name.isBlank()) {
            throw new PlanException("Plan name must not be blank.");
        }

        String trimmed = name.trim();
        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new PlanException("Invalid plan name: " + name);
        }
        return trimmed;
    }
}
