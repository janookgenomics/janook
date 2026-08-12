package com.janookgenomics.janook.cli;

import com.janookgenomics.janook.core.GuidelineEdition;
import java.io.PrintStream;

/**
 * The {@code janook} command.
 *
 * <p>Two questions so far, both about trust rather than classification: which version of what am I
 * running, and what does this tool believe a criterion says. Classification itself needs the
 * decision tree, which does not exist yet.
 */
public final class Janook {

    private static final String USAGE =
            """
            usage: janook --version
                   janook explain <criterion>
                   janook explain --list""";

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * The command proper, with its streams and its exit status passed in rather than reached for,
     * so that it can be tested without exiting the JVM or capturing global state.
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && "--version".equals(args[0])) {
            printVersion(out);
            return ExitStatus.OK;
        }
        if (args.length == 2 && "explain".equals(args[0])) {
            return "--list".equals(args[1])
                    ? ExplainCommand.list(out)
                    : ExplainCommand.run(args[1], out, err);
        }
        err.println(USAGE);
        return ExitStatus.USAGE_ERROR;
    }

    /**
     * Prints the three facts a stored classification needs: which software, which rulebook, and
     * which exact state of the source.
     */
    private static void printVersion(PrintStream out) {
        GuidelineEdition edition = GuidelineEdition.current();
        out.println("janook " + ToolVersion.read());
        out.println(edition.identifier() + " (" + edition.doiUrl() + ")");
        out.println("build " + BuildStamp.read().describe());
    }

    private Janook() {}
}
