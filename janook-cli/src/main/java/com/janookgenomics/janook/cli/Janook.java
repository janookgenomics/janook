package com.janookgenomics.janook.cli;

import com.janookgenomics.janook.core.GuidelineEdition;
import java.io.PrintStream;

/**
 * The {@code janook} command.
 *
 * <p>Today it answers one question — which version of what am I running — because a classification
 * that cannot name the tool and the guideline edition that produced it is not reproducible.
 */
public final class Janook {

    /** Conventional exit status for a usage error, distinct from a run that failed on its merits. */
    private static final int USAGE_ERROR = 2;

    private static final String USAGE = "usage: janook --version";

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
            return 0;
        }
        err.println(USAGE);
        return USAGE_ERROR;
    }

    /**
     * Prints the tool version and the guideline edition. The build commit — the third fact that
     * turns a version into an exact state of the source — arrives with the build stamp.
     */
    private static void printVersion(PrintStream out) {
        GuidelineEdition edition = GuidelineEdition.current();
        out.println("janook " + ToolVersion.read());
        out.println(edition.identifier() + " (" + edition.doiUrl() + ")");
    }

    private Janook() {}
}
