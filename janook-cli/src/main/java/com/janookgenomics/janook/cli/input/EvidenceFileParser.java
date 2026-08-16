package com.janookgenomics.janook.cli.input;

import com.janookgenomics.janook.cli.profile.ShippedProfiles;
import com.janookgenomics.janook.cli.profile.SpeciesProfile;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Reads one evidence file into a {@link VariantInput}, rejecting anything it does not fully
 * understand.
 *
 * <p>This is the tool's front door: the evidence file is the one thing a user hands janook, and
 * everything the tool concludes traces back to it. So the same contract the profile loader set
 * holds here, with more at stake — every fault names the file and the position where one exists,
 * an unrecognised field is an error rather than a shrug, and a state that is not one of the three
 * is never guessed at. A parser that guesses produces classifications nobody can defend.
 *
 * <p>The file has two blocks. {@code variant} says which variant this is; {@code criteria} says
 * what the person decided, one entry per assessed criterion, keyed by criterion code. A criterion
 * the file does not mention is {@code not_assessed}. The {@code met} field takes {@code true},
 * {@code false} or {@code not_assessed} — and the difference between the last two is load-bearing:
 * "we checked and it does not apply" is evidence, "nobody looked" is a gap in the work.
 *
 * <p>A justification — {@code evidence}, {@code source}, {@code asserted_by} — may accompany any
 * state, and is optional even for a met criterion. Whether it should be required there is a
 * deliberately open policy question, parked until the report makes a bare "met" visible.
 */
public final class EvidenceFileParser {

    private static final Set<String> ROOT_FIELDS = Set.of("variant", "criteria");

    private static final Set<String> VARIANT_FIELDS =
            Set.of("species", "gene", "transcript", "hgvs_c", "hgvs_p", "consequence");

    private static final Set<String> CRITERION_FIELDS =
            Set.of("met", "evidence", "source", "asserted_by");

    /**
     * @throws UncheckedIOException if the file cannot be read at all
     * @throws IllegalArgumentException if the content is not a valid evidence file — the message
     *     names the file, and the field or position at fault
     */
    public static VariantInput parse(Path file) {
        Objects.requireNonNull(file, "file");
        try (Reader content = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(content, file.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read evidence file " + file, e);
        }
    }

    /**
     * @param source where the content came from, used in every error message
     */
    public static VariantInput parse(Reader content, String source) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(source, "source");

        Object root;
        try {
            // Duplicate keys are refused rather than last-one-wins: two entries for the same
            // criterion mean two people disagreed, or the file has a bug, and keeping the last
            // silently would hide both.
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            root = new Yaml(new SafeConstructor(options)).load(content);
        } catch (MarkedYAMLException e) {
            throw fault(source, at(e.getProblemMark()) + e.getProblem());
        } catch (YAMLException e) {
            throw fault(source, e.getMessage());
        }

        Map<String, Object> fields = mapOf(root, source, "the evidence file");
        rejectUnknown(fields, ROOT_FIELDS, source, "");

        VariantInput.Builder builder = VariantInput.forVariant(identity(fields, source));
        criteria(fields, source, builder);
        return builder.build();
    }

    private static VariantIdentity identity(Map<String, Object> fields, String source) {
        Map<String, Object> variant =
                mapOf(required(fields, "variant", source), source, "variant");
        rejectUnknown(variant, VARIANT_FIELDS, source, "variant.");

        String speciesName = text(variant, "species", source);
        SpeciesProfile species;
        try {
            species = ShippedProfiles.load(speciesName);
        } catch (IllegalArgumentException e) {
            // The lookup's message already names what was asked and lists what janook knows;
            // the parser adds where it was asked.
            throw fault(source, e.getMessage());
        }

        try {
            return new VariantIdentity(
                    species,
                    text(variant, "gene", source),
                    text(variant, "transcript", source),
                    text(variant, "hgvs_c", source),
                    optionalText(variant, "hgvs_p", source),
                    text(variant, "consequence", source));
        } catch (IllegalArgumentException e) {
            throw fault(source, e.getMessage());
        }
    }

    /** The criteria block is optional: a file mentioning no criteria is all not-assessed. */
    private static void criteria(
            Map<String, Object> fields, String source, VariantInput.Builder builder) {
        Object block = fields.get("criteria");
        if (block == null) {
            return;
        }
        Map<String, Object> entries = mapOf(block, source, "criteria");
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            String code = entry.getKey();
            Criterion criterion =
                    Avcg2024.byCode(code)
                            .orElseThrow(
                                    () ->
                                            fault(
                                                    source,
                                                    "no criterion "
                                                            + code
                                                            + " in "
                                                            + Avcg2024.edition().identifier()
                                                            + " (janook explain --list prints"
                                                            + " all 23)"));

            String where = "criteria." + code;
            Map<String, Object> parts = mapOf(entry.getValue(), source, where);
            rejectUnknown(parts, CRITERION_FIELDS, source, where + ".");

            AssertionState state = state(parts, where, source);
            Justification justification = justification(parts, where, source);
            try {
                if (justification == null) {
                    switch (state) {
                        case MET -> builder.met(criterion);
                        case NOT_MET -> builder.notMet(criterion);
                        case NOT_ASSESSED -> builder.notAssessed(criterion);
                    }
                } else {
                    switch (state) {
                        case MET -> builder.met(criterion, justification);
                        case NOT_MET -> builder.notMet(criterion, justification);
                        case NOT_ASSESSED -> builder.notAssessed(criterion, justification);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw fault(source, e.getMessage());
            }
        }
    }

    private static AssertionState state(Map<String, Object> parts, String where, String source) {
        Object met = parts.get("met");
        if (met == null) {
            throw fault(source, where + " is missing its met field");
        }
        if (Boolean.TRUE.equals(met)) {
            return AssertionState.MET;
        }
        if (Boolean.FALSE.equals(met)) {
            return AssertionState.NOT_MET;
        }
        if ("not_assessed".equals(met)) {
            return AssertionState.NOT_ASSESSED;
        }
        throw fault(
                source,
                where + ".met must be true, false or not_assessed, got: " + met);
    }

    private static Justification justification(
            Map<String, Object> parts, String where, String source) {
        Optional<String> evidence = optionalText(parts, "evidence", source);
        Optional<String> cited = optionalText(parts, "source", source);
        Optional<String> assertedBy = optionalText(parts, "asserted_by", source);
        if (evidence.isEmpty() && cited.isEmpty() && assertedBy.isEmpty()) {
            return null;
        }
        try {
            return new Justification(evidence, cited, assertedBy);
        } catch (IllegalArgumentException e) {
            throw fault(source, where + ": " + e.getMessage());
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

    private static Optional<String> optionalText(
            Map<String, Object> fields, String field, String source) {
        Object value = fields.get(field);
        if (value == null) {
            return Optional.empty();
        }
        if (!(value instanceof String string)) {
            throw fault(source, field + " must be text, got: " + value);
        }
        return Optional.of(string);
    }

    /** Preserves the file's own order, so a file with several faults reports the same one first. */
    private static Map<String, Object> mapOf(Object value, String source, String what) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw fault(source, what + " must be a mapping of fields to values, got: " + value);
        }
        Map<String, Object> fields = new LinkedHashMap<>();
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
        return new IllegalArgumentException("evidence file " + source + ": " + problem);
    }

    private EvidenceFileParser() {}
}
