/**
 * The record, and its renderings.
 *
 * The record is everything janook knows about one classification: the variant's input, the
 * classification itself, and the provenance. The renderings — terminal summary, JSON, the report
 * document — only show what the record holds. Nothing is computed, looked up or decided during
 * rendering, and nothing here reads a clock: rendering is a pure function, which is what makes
 * identical records produce identical bytes.
 */
package com.janookgenomics.janook.cli.report;
