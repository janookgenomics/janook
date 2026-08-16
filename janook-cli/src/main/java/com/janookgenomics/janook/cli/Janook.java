package com.janookgenomics.janook.cli;

import com.janookgenomics.janook.core.GuidelineEdition;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * The {@code janook} command.
 *
 * <p>Every subcommand wires together pieces built and tested elsewhere; nothing decided here is
 * about classification. What this class owns is the surface: which commands exist, the help text,
 * and the four exit codes a script can branch on.
 */
public final class Janook {

    /**
     * The whole surface, printed by {@code janook help} (exit 0 — asking is an answer) and by a
     * command line that could not be understood (exit 2 — the same text, but a typo is not a
     * success).
     */
    static final String HELP =
            """
            usage: janook <command>

              janook --version                    tool version, guideline edition, build commit
              janook init                         print an evidence-file template to fill in
              janook explain <criterion>          one criterion, exactly as encoded
              janook explain --list               all 23 criteria, one line each
              janook classify <file>              classify one variant; prints the summary
              janook classify --batch <file.tsv>  classify a spreadsheet, one line per variant
              janook help                         this text

            classify flags:
              --json               print the JSON document instead of the summary
              --report             print the Markdown report instead of the summary
              --brief              print only the classification line; with --json, a
                                   minimal {label, reason} document
              --operator <name>    record who ran the classification

            exit codes:
              0  answered — an uncertain classification is an answer
              1  input rejected — the message says what to fix
              2  command not understood
              3  internal error — janook's own bug; please report it""";

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * The command proper, with its streams and its exit status passed in rather than reached for,
     * so that it can be tested without exiting the JVM or capturing global state.
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        return guard(() -> dispatch(args, out, err), err);
    }

    private static int dispatch(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && "--version".equals(args[0])) {
            printVersion(out);
            return ExitStatus.OK;
        }
        if (args.length == 1 && "init".equals(args[0])) {
            return InitCommand.run(out);
        }
        if (args.length == 1 && "help".equals(args[0])) {
            out.println(HELP);
            return ExitStatus.OK;
        }
        if (args.length == 2 && "explain".equals(args[0])) {
            return "--list".equals(args[1])
                    ? ExplainCommand.list(out)
                    : ExplainCommand.run(args[1], out, err);
        }
        if (args.length >= 1 && "classify".equals(args[0])) {
            return ClassifyCommand.run(Arrays.copyOfRange(args, 1, args.length), out, err);
        }
        err.println(HELP);
        return ExitStatus.USAGE_ERROR;
    }

    /**
     * The last resort: anything a command did not expect and did not handle is janook's own bug,
     * and it must never read as "your input was bad". The stack trace still prints — it is what
     * makes the bug report useful — but under a plain statement of whose failure this is.
     */
    static int guard(Command command, PrintStream err) {
        try {
            return command.run();
        } catch (RuntimeException | Error e) {
            err.println(
                    "janook: internal error — this is a bug in janook, not a problem with your"
                            + " input.");
            err.println(
                    "Please report it, with the command you ran and the trace below:"
                            + " https://github.com/janookgenomics/janook/issues");
            e.printStackTrace(err);
            return ExitStatus.INTERNAL_ERROR;
        }
    }

    /** A command invocation, for {@link #guard}. */
    interface Command {
        int run();
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
