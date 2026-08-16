package com.janookgenomics.janook.cli.input;

import com.janookgenomics.janook.cli.profile.ShippedProfiles;
import com.janookgenomics.janook.cli.profile.SpeciesProfile;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import com.janookgenomics.janook.core.evidence.AssertionState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a TSV file with one variant per row, because the audience lives in spreadsheets, and each
 * row becomes exactly what the YAML parser would have produced for it.
 *
 * <p>The format: a header row naming the columns, then one variant per row. The identity columns
 * are {@code species}, {@code gene}, {@code transcript}, {@code hgvs_c}, {@code consequence}
 * (all required) and {@code hgvs_p} (optional). Beyond those, any criterion code is a column, and
 * its cells take {@code true}, {@code false}, {@code not_assessed} — case-insensitively, because
 * spreadsheets write {@code TRUE} — or nothing at all: <strong>an empty cell means not
 * assessed</strong>, the natural spreadsheet spelling of "nobody looked". Rows may mix species.
 *
 * <p>What this format deliberately cannot say: justification prose. There is no workable column
 * for per-criterion evidence text, sources and operators, so a batch carries states only — a
 * variant whose justifications matter belongs in an evidence file, where they have room. This is
 * a stated limit of the format, not a silent one.
 *
 * <p>Cells are taken verbatim apart from surrounding whitespace; there is no quoting, and a cell
 * cannot contain a tab. A row with fewer cells than columns reads the missing trailing cells as
 * empty, because spreadsheet exports drop them; a row with more cells than columns is rejected.
 * A fault anywhere rejects the whole file naming the line and column — a batch is never returned
 * with broken rows quietly missing, because an answer that looks complete and is not is the worst
 * answer this tool could give.
 */
public final class EvidenceBatchParser {

    private static final Set<String> IDENTITY_COLUMNS =
            Set.of("species", "gene", "transcript", "hgvs_c", "hgvs_p", "consequence");

    private static final List<String> REQUIRED_COLUMNS =
            List.of("species", "gene", "transcript", "hgvs_c", "consequence");

    /**
     * @throws UncheckedIOException if the file cannot be read at all
     * @throws IllegalArgumentException if any part of the content is not a valid batch — the
     *     message names the file, the line and where possible the column at fault
     */
    public static List<VariantInput> parse(Path file) {
        Objects.requireNonNull(file, "file");
        try (Reader content = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(content, file.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read batch file " + file, e);
        }
    }

    /**
     * @param source where the content came from, used in every error message
     */
    public static List<VariantInput> parse(Reader content, String source) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(source, "source");

        List<String> lines;
        try (BufferedReader reader = new BufferedReader(content)) {
            lines = reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read batch " + source, e);
        }

        int headerLine = firstNonBlank(lines, 0);
        if (headerLine < 0) {
            throw fault(source, "the file is empty — no header row");
        }
        List<String> columns = header(lines.get(headerLine), source);

        List<VariantInput> batch = new ArrayList<>();
        for (int i = headerLine + 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            batch.add(row(lines.get(i), i + 1, columns, source));
        }
        if (batch.isEmpty()) {
            throw fault(
                    source,
                    "the file has a header but no variant rows — nothing here classifies");
        }
        return List.copyOf(batch);
    }

    private static List<String> header(String line, String source) {
        List<String> columns = new ArrayList<>();
        for (String cell : line.split("\t", -1)) {
            String column = cell.strip();
            if (column.isEmpty()) {
                throw fault(source, "the header row has an unnamed column");
            }
            if (!IDENTITY_COLUMNS.contains(column) && Avcg2024.byCode(column).isEmpty()) {
                throw fault(
                        source,
                        "unrecognised column: " + column
                                + " (identity columns and "
                                + Avcg2024.edition().identifier()
                                + " criterion codes are accepted)");
            }
            if (columns.contains(column)) {
                throw fault(source, "the header names " + column + " twice");
            }
            columns.add(column);
        }
        for (String required : REQUIRED_COLUMNS) {
            if (!columns.contains(required)) {
                throw fault(source, "the header is missing the " + required + " column");
            }
        }
        return columns;
    }

    private static VariantInput row(
            String line, int lineNumber, List<String> columns, String source) {
        String[] split = line.split("\t", -1);
        if (split.length > columns.size()) {
            throw fault(
                    source,
                    "line " + lineNumber + " has " + split.length + " cells for "
                            + columns.size() + " columns");
        }

        Map<String, String> cells = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            // Fewer cells than columns is a spreadsheet export dropping trailing tabs; the
            // missing cells are empty, which for a criterion column means not assessed.
            cells.put(columns.get(i), i < split.length ? split[i].strip() : "");
        }

        VariantInput.Builder builder =
                VariantInput.forVariant(identity(cells, lineNumber, source));
        for (String column : columns) {
            Avcg2024.byCode(column)
                    .ifPresent(
                            criterion ->
                                    decide(
                                            builder,
                                            criterion,
                                            cells.get(column),
                                            lineNumber,
                                            source));
        }
        return builder.build();
    }

    private static VariantIdentity identity(
            Map<String, String> cells, int lineNumber, String source) {
        for (String required : REQUIRED_COLUMNS) {
            if (cells.get(required).isEmpty()) {
                throw fault(
                        source, "line " + lineNumber + ": the " + required + " cell is empty");
            }
        }

        SpeciesProfile species;
        try {
            species = ShippedProfiles.load(cells.get("species"));
        } catch (IllegalArgumentException e) {
            throw fault(source, "line " + lineNumber + ": " + e.getMessage());
        }

        String hgvsP = cells.getOrDefault("hgvs_p", "");
        try {
            return new VariantIdentity(
                    species,
                    cells.get("gene"),
                    cells.get("transcript"),
                    cells.get("hgvs_c"),
                    hgvsP.isEmpty() ? Optional.empty() : Optional.of(hgvsP),
                    cells.get("consequence"));
        } catch (IllegalArgumentException e) {
            throw fault(source, "line " + lineNumber + ": " + e.getMessage());
        }
    }

    private static void decide(
            VariantInput.Builder builder,
            Criterion criterion,
            String cell,
            int lineNumber,
            String source) {
        if (cell.isEmpty()) {
            return; // an empty cell is not assessed, which is already every criterion's default
        }
        AssertionState state =
                switch (cell.toLowerCase(Locale.ROOT)) {
                    case "true" -> AssertionState.MET;
                    case "false" -> AssertionState.NOT_MET;
                    case "not_assessed" -> AssertionState.NOT_ASSESSED;
                    default ->
                            throw fault(
                                    source,
                                    "line " + lineNumber + ", column " + criterion.code()
                                            + ": must be true, false, not_assessed or empty,"
                                            + " got: " + cell);
                };
        try {
            switch (state) {
                case MET -> builder.met(criterion);
                case NOT_MET -> builder.notMet(criterion);
                case NOT_ASSESSED -> builder.notAssessed(criterion);
            }
        } catch (IllegalArgumentException e) {
            throw fault(source, "line " + lineNumber + ": " + e.getMessage());
        }
    }

    private static int firstNonBlank(List<String> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                return i;
            }
        }
        return -1;
    }

    private static IllegalArgumentException fault(String source, String problem) {
        return new IllegalArgumentException("batch " + source + ": " + problem);
    }

    private EvidenceBatchParser() {}
}
