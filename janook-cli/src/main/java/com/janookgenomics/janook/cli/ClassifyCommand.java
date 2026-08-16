package com.janookgenomics.janook.cli;

import com.janookgenomics.janook.cli.input.EvidenceBatchParser;
import com.janookgenomics.janook.cli.input.EvidenceFileParser;
import com.janookgenomics.janook.cli.input.VariantInput;
import com.janookgenomics.janook.cli.report.ClassificationRecord;
import com.janookgenomics.janook.cli.report.JsonReport;
import com.janookgenomics.janook.cli.report.MarkdownReport;
import com.janookgenomics.janook.cli.report.Provenance;
import com.janookgenomics.janook.cli.report.TerminalSummary;
import com.janookgenomics.janook.core.decision.Classification;
import com.janookgenomics.janook.core.decision.Classifier;
import com.janookgenomics.janook.core.decision.RuleMatch;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * {@code janook classify} — an evidence file in, the answer out.
 *
 * <p>Everything this command does existed and was tested before it: the parsers, the engine, the
 * record, the renderings. What only a command can know, it supplies: the hash of the input, the
 * date of the run, and — only when {@code --operator} names one — who ran it. The file is read
 * once, as bytes; the hash is computed from exactly the bytes that get parsed, so the provenance
 * can never describe a different file than the one classified.
 *
 * <p>One artifact per run, always to standard output: the summary by default, the JSON document
 * with {@code --json}, the report with {@code --report}. Saving is redirection. Everything that
 * is not the artifact goes to standard error, so a redirected artifact is never contaminated.
 */
final class ClassifyCommand {

    private enum Artifact {
        SUMMARY,
        JSON,
        REPORT
    }

    /** Runs with the arguments after {@code classify}. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        Path file = null;
        boolean batch = false;
        boolean json = false;
        boolean report = false;
        Optional<String> operator = Optional.empty();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--batch" -> batch = true;
                case "--json" -> json = true;
                case "--report" -> report = true;
                case "--operator" -> {
                    if (i + 1 >= args.length || args[i + 1].isBlank()) {
                        return usage(err);
                    }
                    operator = Optional.of(args[++i]);
                }
                default -> {
                    if (args[i].startsWith("--") || file != null) {
                        return usage(err);
                    }
                    file = Path.of(args[i]);
                }
            }
        }
        if (file == null || (json && report) || (batch && report)) {
            return usage(err);
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            err.println("janook: cannot read " + file + ": " + e.getMessage());
            return ExitStatus.REJECTED_INPUT;
        }

        Provenance provenance =
                new Provenance(ToolVersion.read(), hash(bytes), LocalDate.now(), operator);
        Artifact artifact = json ? Artifact.JSON : report ? Artifact.REPORT : Artifact.SUMMARY;

        try {
            String content = new String(bytes, StandardCharsets.UTF_8);
            return batch
                    ? classifyBatch(content, file, provenance, artifact, out)
                    : classifyOne(content, file, provenance, artifact, out);
        } catch (IllegalArgumentException | UncheckedIOException e) {
            err.println("janook: " + e.getMessage());
            return ExitStatus.REJECTED_INPUT;
        }
    }

    private static int classifyOne(
            String content, Path file, Provenance provenance, Artifact artifact,
            PrintStream out) {
        VariantInput input = EvidenceFileParser.parse(new StringReader(content), file.toString());
        ClassificationRecord record =
                ClassificationRecord.classify(input, Classifier.standard(), provenance);

        out.print(
                switch (artifact) {
                    case SUMMARY -> TerminalSummary.render(record);
                    case JSON -> JsonReport.render(record);
                    case REPORT -> MarkdownReport.render(record);
                });
        return ExitStatus.OK;
    }

    private static int classifyBatch(
            String content, Path file, Provenance provenance, Artifact artifact,
            PrintStream out) {
        List<VariantInput> batch =
                EvidenceBatchParser.parse(new StringReader(content), file.toString());

        // Classify every row before printing anything: a fault mid-way must never leave a
        // half-written artifact on standard output.
        List<ClassificationRecord> records = new ArrayList<>();
        for (VariantInput input : batch) {
            records.add(ClassificationRecord.classify(input, Classifier.standard(), provenance));
        }

        if (artifact == Artifact.JSON) {
            StringBuilder array = new StringBuilder("[\n");
            for (int i = 0; i < records.size(); i++) {
                if (i > 0) {
                    array.append(",\n");
                }
                array.append(JsonReport.render(records.get(i)));
            }
            out.print(array.append("]\n"));
            return ExitStatus.OK;
        }

        for (ClassificationRecord record : records) {
            out.println(line(record));
        }
        return ExitStatus.OK;
    }

    /** One scannable line: the gene, the variant, the label, and what decided it. */
    private static String line(ClassificationRecord record) {
        Classification classification = record.classification();
        String label = classification.label().name().replace('_', ' ');
        String decided =
                switch (classification.reason()) {
                    case ONE_BRANCH_LABELLED -> {
                        RuleMatch match =
                                classification
                                        .pathogenic()
                                        .or(classification::benign)
                                        .orElseThrow();
                        yield "rule "
                                + match.rule()
                                + match.clause().map(c -> " (" + c + ")").orElse("");
                    }
                    case NOT_ENOUGH_CRITERIA -> "not enough criteria were met";
                    case CONFLICTING_BRANCHES -> "the evidence contradicts itself";
                };
        return record.input().identity().gene()
                + "  "
                + record.input().identity().hgvsC()
                + "  "
                + label
                + " — "
                + decided;
    }

    private static String hash(byte[] bytes) {
        try {
            return "sha256:"
                    + HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            // Every JVM ships SHA-256; a missing one is an internal failure, not bad input.
            throw new IllegalStateException("SHA-256 is unavailable in this JVM", e);
        }
    }

    private static int usage(PrintStream err) {
        err.println(Janook.HELP);
        return ExitStatus.USAGE_ERROR;
    }

    private ClassifyCommand() {}
}
