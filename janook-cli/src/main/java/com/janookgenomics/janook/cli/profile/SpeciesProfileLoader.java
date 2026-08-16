package com.janookgenomics.janook.cli.profile;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Reads one profile file into a {@link SpeciesProfile}, rejecting anything it does not fully
 * understand.
 *
 * <p>Every fault names the file, and a syntax fault names the line and column too. A field the
 * loader does not recognise is a rejection, not a shrug: a tolerant loader would turn a mistyped
 * optional field into one that silently never applies, and the person who wrote it deserves an
 * error rather than a profile that lacks what they configured.
 *
 * <p>The format is YAML with {@code snake_case} keys — the same syntax the variant input will use,
 * so the tool has one config syntax rather than two. Both predictor lists must be present even
 * when empty: an explicit empty list can only mean "no predictors are validated for this species",
 * where an absent one could also mean "forgot to fill it in".
 */
public final class SpeciesProfileLoader {

    private static final Set<String> ROOT_FIELDS =
            Set.of("species", "display_name", "assembly", "annotation", "omia_species",
                    "predictors");

    private static final Set<String> PREDICTOR_FIELDS = Set.of("missense", "splice");

    /**
     * @throws UncheckedIOException if the file cannot be read at all
     * @throws IllegalArgumentException if the content is not a valid profile — the message names
     *     the file, and the field or position at fault
     */
    public static SpeciesProfile load(Path file) {
        Objects.requireNonNull(file, "file");
        try (Reader content = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return load(content, file.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read profile " + file, e);
        }
    }

    /**
     * @param source where the content came from, used in every error message — a path, or later a
     *     classpath resource name when the shipped profiles load from inside the jar
     */
    public static SpeciesProfile load(Reader content, String source) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(source, "source");

        Object root;
        try {
            root = new Yaml(new SafeConstructor(new LoaderOptions())).load(content);
        } catch (MarkedYAMLException e) {
            throw fault(source, at(e.getProblemMark()) + e.getProblem());
        } catch (YAMLException e) {
            throw fault(source, e.getMessage());
        }

        Map<String, Object> fields = mapOf(root, source, "the profile");
        rejectUnknown(fields, ROOT_FIELDS, source, "");
        Map<String, Object> predictors =
                mapOf(required(fields, "predictors", source), source, "predictors");
        rejectUnknown(predictors, PREDICTOR_FIELDS, source, "predictors.");

        try {
            return new SpeciesProfile(
                    text(fields, "species", source),
                    text(fields, "display_name", source),
                    text(fields, "assembly", source),
                    text(fields, "annotation", source),
                    wholeNumber(fields, "omia_species", source),
                    tools(predictors, "missense", source),
                    tools(predictors, "splice", source));
        } catch (IllegalArgumentException e) {
            // The profile's own validation names the field; the loader adds where it was read.
            throw fault(source, e.getMessage());
        }
    }

    private static Object required(Map<String, Object> fields, String field, String source) {
        Object value = fields.get(field);
        if (value == null) {
            throw fault(source, "missing required field: " + field);
        }
        return value;
    }

    private static String text(Map<String, Object> fields, String field, String source) {
        Object value = required(fields, field, source);
        if (!(value instanceof String string)) {
            throw fault(source, field + " must be text, got: " + value);
        }
        return string;
    }

    private static int wholeNumber(Map<String, Object> fields, String field, String source) {
        Object value = required(fields, field, source);
        if (!(value instanceof Integer number)) {
            throw fault(source, field + " must be a whole number, got: " + value);
        }
        return number;
    }

    private static List<String> tools(Map<String, Object> predictors, String kind, String source) {
        Object value = predictors.get(kind);
        if (value == null) {
            throw fault(
                    source,
                    "missing required field: predictors." + kind
                            + " (an empty list [] is how to say none are validated)");
        }
        if (!(value instanceof List<?> list)) {
            throw fault(source, "predictors." + kind + " must be a list, got: " + value);
        }
        for (Object tool : list) {
            if (!(tool instanceof String)) {
                throw fault(source, "predictors." + kind + " contains a non-text entry: " + tool);
            }
        }
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) list;
        return names;
    }

    private static Map<String, Object> mapOf(Object value, String source, String what) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw fault(source, what + " must be a mapping of fields to values, got: " + value);
        }
        Map<String, Object> fields = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw fault(source, what + " has a non-text field name: " + entry.getKey());
            }
            fields.put(key, entry.getValue());
        }
        return fields;
    }

    private static void rejectUnknown(
            Map<String, Object> fields, Set<String> known, String source, String prefix) {
        for (String field : fields.keySet()) {
            if (!known.contains(field)) {
                throw fault(
                        source,
                        "unrecognised field: " + prefix + field + " (known: "
                                + String.join(", ", known.stream().sorted().toList()) + ")");
            }
        }
    }

    private static String at(Mark mark) {
        if (mark == null) {
            return "";
        }
        return "line " + (mark.getLine() + 1) + ", column " + (mark.getColumn() + 1) + ": ";
    }

    private static IllegalArgumentException fault(String source, String problem) {
        return new IllegalArgumentException("profile " + source + ": " + problem);
    }

    private SpeciesProfileLoader() {}
}
