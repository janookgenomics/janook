package com.janookgenomics.janook.cli;

import com.janookgenomics.janook.core.GuidelineEdition;
import com.janookgenomics.janook.core.criteria.AcmgOrigin;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code janook explain <code>} — what a criterion is, what it weighs, and what it was in ACMG/AMP.
 *
 * <p>This is the audit surface. A published file can always be something the engine ignores;
 * this prints from the same model that will classify variants, so what it says is what the tool
 * will do. It is also the answer to the question most of this tool's users will actually have,
 * which is not "what is PS5" but "what changed from the human guidelines".
 */
final class ExplainCommand {

    /** Terminal-friendly, and narrow enough to stay readable in a split window. */
    private static final int WRAP = 76;

    private static final String INDENT = "  ";

    /**
     * @return an exit status: clean, or input rejected because no such criterion exists
     */
    static int run(String rawCode, PrintStream out, PrintStream err) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        Optional<Criterion> found = Avcg2024.byCode(code);

        if (found.isEmpty()) {
            err.println(
                    "janook: no criterion " + rawCode + " in " + Avcg2024.edition().identifier());

            // The likeliest miss by far is a human code that AVCG renumbered, and we can answer it
            // exactly rather than guess: BP7 is BP6, PP1 is PS5, PM5 is PM2.
            //
            // Nothing beyond that. Anything else is a guess at what someone meant out of a list of
            // twenty-three that --list prints in full, on the next line.
            whatAcmgCodeBecame(code)
                    .ifPresent(
                            criterion ->
                                    err.println(
                                            "  ACMG/AMP "
                                                    + code
                                                    + " is "
                                                    + Avcg2024.edition().identifier()
                                                    + " "
                                                    + criterion.code()
                                                    + "."));

            err.println("  all 23 criteria: janook explain --list");
            return ExitStatus.REJECTED_INPUT;
        }

        print(found.get(), out);
        return ExitStatus.OK;
    }

    /** {@code janook explain --list} — the inventory, for someone who does not know the codes. */
    static int list(PrintStream out) {
        for (Criterion criterion : Avcg2024.all()) {
            out.printf(
                    "%-5s %-10s %-11s %s%n",
                    criterion.code(),
                    criterion.direction().name().toLowerCase(Locale.ROOT),
                    criterion.weight().label(),
                    preview(criterion.definition()));
        }
        return ExitStatus.OK;
    }

    private static void print(Criterion criterion, PrintStream out) {
        GuidelineEdition edition = Avcg2024.edition();

        out.printf(
                "%s  %s  %s%n%n",
                criterion.code(),
                criterion.direction().name().toLowerCase(Locale.ROOT),
                criterion.weight().label());

        for (String line : wrap(criterion.definition(), WRAP - INDENT.length())) {
            out.println(INDENT + line);
        }

        out.println();
        // Labelled as ours, for the same reason the generated reference is: the definition is the
        // guideline's words and this line is not. On its own line so wrapping can never split the
        // disclaimer away from what it disclaims.
        labelled(out, "Origin", criterion.acmgOrigin().describe());
        out.println(gutter("Origin") + "(our annotation, not from either paper)");
        labelled(
                out,
                "Source",
                criterion.transcribedFrom()
                        + " · "
                        + edition.identifier()
                        + " · "
                        + edition.doiUrl());

        // AVCG reused this code for a criterion unrelated to ACMG/AMP's criterion of the same
        // name. Without a pointer, a reader who knows the human guidelines will assume the two
        // are the same thing. Today only PP1 has this problem.
        whatAcmgCodeBecame(criterion.code())
                .filter(acmg -> !acmg.code().equals(criterion.code()))
                .ifPresent(
                        acmg -> {
                            out.println();
                            String note =
                                    "Note: ACMG/AMP's "
                                            + criterion.code()
                                            + " is a different criterion. In AVCG it is "
                                            + acmg.code()
                                            + ".";
                            for (String line : wrap(note, WRAP - INDENT.length())) {
                                out.println(INDENT + line);
                            }
                        });
    }

    /**
     * A labelled line, wrapped with a hanging indent so the continuation lines up under the value
     * rather than under the label. Origin notes are long enough to need it — one of them is over
     * 150 characters.
     */
    private static void labelled(PrintStream out, String label, String value) {
        String gutter = gutter(label);
        List<String> lines = wrap(value, WRAP - gutter.length());
        out.println(INDENT + label + "  " + lines.getFirst());
        for (String continuation : lines.subList(1, lines.size())) {
            out.println(gutter + continuation);
        }
    }

    /**
     * The AVCG criterion that carries a given ACMG/AMP code, if one does.
     *
     * <p>Exhaustive over {@link AcmgOrigin} by construction — the compiler refuses this switch if a
     * new kind of provenance is added and not handled here.
     */
    private static Optional<Criterion> whatAcmgCodeBecame(String acmgCode) {
        return Avcg2024.all().stream()
                .filter(
                        criterion ->
                                switch (criterion.acmgOrigin()) {
                                    case AcmgOrigin.Retained(String code) -> code.equals(acmgCode);
                                    case AcmgOrigin.Amended(String code, var ignored) ->
                                            code.equals(acmgCode);
                                    case AcmgOrigin.Renumbered(String code, var ignored) ->
                                            code.equals(acmgCode);
                                    case AcmgOrigin.NewInAvcg ignored -> false;
                                })
                .findFirst();
    }

    /** Where a continuation line starts: under the value, not under the label. */
    private static String gutter(String label) {
        return INDENT + " ".repeat(label.length() + 2);
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Enough of a definition to recognise it in a list, without wrapping the terminal.
     *
     * <p>Cut on length alone. Cutting at the first comma looked tidier and turned {@code PVS1} into
     * "Null variant (nonsense" — a truncation that reads like a complete thought is worse than one
     * that obviously is not.
     */
    private static String preview(String definition) {
        return definition.length() <= 60 ? definition : definition.substring(0, 57) + "...";
    }

    private ExplainCommand() {}
}
