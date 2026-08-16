/**
 * The in-memory form of an evidence file: which variant this is, and what a person decided about
 * each criterion, with their reasons.
 *
 * The evidence file is the one thing a user hands janook. This package holds what it becomes once
 * parsed; the parsing itself arrives separately. The engine sees only the criterion decisions —
 * the variant's identity and the justification prose travel around it, into the report.
 */
package com.janookgenomics.janook.cli.input;
