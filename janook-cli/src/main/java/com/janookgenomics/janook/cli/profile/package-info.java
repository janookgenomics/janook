/**
 * Species profiles: the facts about one species that vary while the engine does not.
 *
 * The engine in janook-core never sees a profile. Profiles are read at the edge, used to validate
 * input and record provenance, and — once predictor adapters exist — to say which prediction tools
 * are valid for a species.
 */
package com.janookgenomics.janook.cli.profile;
