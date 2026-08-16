package com.janookgenomics.janook.cli;

import com.janookgenomics.janook.cli.profile.ShippedProfiles;
import com.janookgenomics.janook.core.criteria.Avcg2024;
import com.janookgenomics.janook.core.criteria.Criterion;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code janook init} — prints an evidence-file template, so nobody starts from a blank page or a
 * stale example copied from somewhere.
 *
 * <p>Printing to standard output is the whole design: {@code janook init > variant.yaml} creates
 * the file, and the command itself never touches the filesystem. The template is generated from
 * the criterion model, the same discipline as the criteria reference — every criterion appears as
 * a commented stub with its code, direction, weight and the start of its definition, so the file
 * documents itself. Uncommenting a stub means removing the leading {@code #} from its two lines;
 * a stub left commented is simply not assessed, which parses fine.
 *
 * <p>The stubs deliberately carry only the {@code met} line. Pre-printing empty
 * {@code evidence: ""} lines would hand every user a trap, because a present-but-blank field is a
 * rejection; the header shows the justification fields once, as an example to copy.
 */
final class InitCommand {

    static int run(PrintStream out) {
        out.print(template());
        return ExitStatus.OK;
    }

    static String template() {
        StringBuilder out = new StringBuilder();

        out.append(
                """
                # janook evidence file — record one variant, and your decision on each criterion.
                #
                # How to use this file:
                #   1. Fill in the variant block below.
                #   2. For each criterion you assessed, remove the leading # from its two lines
                #      and set met to one of: true, false, not_assessed.
                #        true          the criterion applies to this variant
                #        false         you checked, and it does not apply
                #        not_assessed  nobody looked (a criterion left commented means the same)
                #   3. Optionally add evidence, source and asserted_by lines under any criterion:
                #        evidence: "Cosegregates with disease in 12 affected animals."
                #        source: "PMID 15340017"
                #        asserted_by: your-name
                #
                """);

        out.append("# Criteria are ")
                .append(Avcg2024.edition().identifier())
                .append(" — janook explain <code> prints any of them in full.\n#\n");

        out.append("# The species must be one of:\n");
        for (String line : wrapped(ShippedProfiles.known())) {
            out.append("#   ").append(line).append('\n');
        }

        out.append(
                """

                variant:
                  species: ""
                  gene: ""
                  transcript: ""
                  hgvs_c: ""
                  hgvs_p: ""      # protein form; delete this line if the variant has none
                  consequence: ""

                criteria:
                """);

        for (Criterion criterion : Avcg2024.all()) {
            out.append("#  ")
                    .append(criterion.code())
                    .append(":")
                    .append(" ".repeat(5 - criterion.code().length()))
                    .append("# ")
                    .append(criterion.direction().name().toLowerCase(Locale.ROOT))
                    .append(", ")
                    .append(criterion.weight().label())
                    .append(": ")
                    .append(ExplainCommand.preview(criterion.definition()))
                    .append('\n')
                    .append("#    met: true\n");
        }

        return out.toString();
    }

    /** The species names as comment lines, wrapped so no line runs long. */
    private static List<String> wrapped(List<String> species) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String name : species) {
            if (!line.isEmpty() && line.length() + name.length() + 2 > 80) {
                lines.add(line.append(",").toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(", ");
            }
            line.append(name);
        }
        lines.add(line.toString());
        return lines;
    }

    private InitCommand() {}
}
