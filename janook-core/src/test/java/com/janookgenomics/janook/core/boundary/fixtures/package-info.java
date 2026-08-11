/**
 * Classes that deliberately break the core boundary.
 *
 * <p>They exist so the boundary rules can be proven to fire. Nothing here is production code and
 * nothing here is imported by production code — these live in test sources, which the production
 * rule explicitly excludes from its scan.
 */
package com.janookgenomics.janook.core.boundary.fixtures;
