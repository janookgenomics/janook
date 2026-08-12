package com.janookgenomics.janook.cli;

/**
 * What the shell learns from a run, without parsing stdout.
 *
 * <p>Three states, deliberately distinct. "You asked for something that does not exist" is not the
 * same event as "you typed the command wrong", and a script that retries the second should not
 * retry the first. This mirrors the convention the check scripts already use.
 */
final class ExitStatus {

    /** Ran, and answered. */
    static final int OK = 0;

    /** Ran, and the answer is that the input is not valid — an unknown criterion, later a bad file. */
    static final int REJECTED_INPUT = 1;

    /** Did not run: the command line itself was not understood. */
    static final int USAGE_ERROR = 2;

    private ExitStatus() {}
}
