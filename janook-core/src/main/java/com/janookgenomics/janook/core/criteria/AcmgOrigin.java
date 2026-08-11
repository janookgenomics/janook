package com.janookgenomics.janook.core.criteria;

import java.util.Objects;

/**
 * What a criterion was in the human ACMG/AMP guidelines, if anything.
 *
 * <p>This is data rather than a comment because it is the single most misleading thing about AVCG:
 * <strong>a shared code is not a shared criterion.</strong> ACMG's {@code PP1} — cosegregation — is
 * AVCG's {@code PS5}, reweighted; AVCG's {@code PP1} is a new criterion about cross-species
 * evidence. Anyone carrying ACMG knowledge across will be wrong twice on that one code, and most of
 * Janook's audience carries ACMG knowledge across.
 *
 * <p>Seven ACMG criteria were removed outright and AVCG renumbered to close the gaps rather than
 * leave holes, which is why so many codes moved. Sixteen changes in total: fourteen removed or
 * altered, two added.
 */
public sealed interface AcmgOrigin {

    /** The ACMG/AMP code this came from, where there is one. */
    String describe();

    /** Same code, same meaning. */
    record Retained(String acmgCode) implements AcmgOrigin {
        public Retained {
            Objects.requireNonNull(acmgCode, "acmgCode");
        }

        @Override
        public String describe() {
            return "ACMG/AMP " + acmgCode;
        }
    }

    /** Same code, but AVCG changed the wording. */
    record Amended(String acmgCode, String amendment) implements AcmgOrigin {
        public Amended {
            Objects.requireNonNull(acmgCode, "acmgCode");
            Objects.requireNonNull(amendment, "amendment");
        }

        @Override
        public String describe() {
            return "ACMG/AMP " + acmgCode + ", amended: " + amendment;
        }
    }

    /** A different code in ACMG/AMP. The one that catches people out. */
    record Renumbered(String acmgCode, String change) implements AcmgOrigin {
        public Renumbered {
            Objects.requireNonNull(acmgCode, "acmgCode");
            Objects.requireNonNull(change, "change");
        }

        @Override
        public String describe() {
            return "was ACMG/AMP " + acmgCode + " — " + change;
        }
    }

    /** No ACMG/AMP equivalent. Do not look for one. */
    record NewInAvcg(String rationale) implements AcmgOrigin {
        public NewInAvcg {
            Objects.requireNonNull(rationale, "rationale");
        }

        @Override
        public String describe() {
            return "new in AVCG — " + rationale;
        }
    }
}
