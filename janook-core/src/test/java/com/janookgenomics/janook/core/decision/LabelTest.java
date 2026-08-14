package com.janookgenomics.janook.core.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabelTest {

    /**
     * The five labels, spelled out. Written as a literal list rather than derived from the enum,
     * for the same reason the criterion inventory test is: a test that computes its expectation
     * from the thing under test asserts only that the code is self-consistent. Adding, removing or
     * renaming a label must fail here and be corrected deliberately — a stored classification
     * carries these names, so a rename changes the meaning of results already recorded.
     */
    private static final List<String> LABELS =
            List.of(
                    "PATHOGENIC",
                    "LIKELY_PATHOGENIC",
                    "UNCERTAIN_SIGNIFICANCE",
                    "LIKELY_BENIGN",
                    "BENIGN");

    @Test
    @DisplayName("the labels are exactly the five of Table 6, in the paper's order")
    void labelsAreExactlyTheFive() {
        assertEquals(LABELS, Arrays.stream(Label.values()).map(Enum::name).toList());
    }
}
