package com.janookgenomics.janook.core.criteria;

import com.janookgenomics.janook.core.GuidelineEdition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The criteria of the {@code AVCG-2024} edition, transcribed by hand from Table 4 of the
 * publication.
 *
 * <p><strong>The definitions are verbatim and must stay that way.</strong> They are the guideline's
 * words, not ours; a paraphrase that reads better is a different criterion. Third-party text, used
 * under CC BY 4.0 — see {@code NOTICE}.
 *
 * <p>Written in Java rather than read from a data file because {@code janook-core} takes no
 * third-party dependency, so no parser can live here. {@code docs/criteria/AVCG-2024.md} is
 * generated from this class and is what a reviewer diffs against the paper; the build fails if that
 * file falls out of step.
 *
 * <p>A future edition arrives as a new class beside this one. Nothing here is ever renamed or
 * edited to mean something else, because classifications already recorded under {@code AVCG-2024}
 * have to keep meaning what they meant.
 *
 * <p>The {@link AcmgOrigin} on each criterion is <em>our</em> annotation, not either paper's. Each
 * one is checked against the 2015 ACMG/AMP guidelines — Richards et al.,
 * <a href="https://doi.org/10.1038/gim.2015.30">10.1038/gim.2015.30</a> — by comparing the two
 * definitions directly, rather than inferred from the codes.
 *
 * <p>All twenty-three: fourteen pathogenic, nine benign. There is no {@code BS4} and no
 * {@code BP7} — AVCG renumbered to close the gaps left by the seven ACMG/AMP criteria it removed,
 * rather than leaving holes in the sequence.
 */
public final class Avcg2024 {

    private static final String TABLE = "Table 4, p. 8";

    /**
     * Retained from ACMG/AMP by code, but AVCG widened it: loss-of-function evidence may come from
     * another species. That is the whole reason these guidelines exist, so it is the first thing
     * transcribed.
     */
    public static final Criterion PVS1 =
            new Criterion(
                    "PVS1",
                    Direction.PATHOGENIC,
                    Weight.VERY_STRONG,
                    """
                    Null variant (nonsense, frameshift, canonical ±1 or 2 splice-sites, initiation \
                    codon, single or multi-exon deletion) in a gene where LOF is a known mechanism \
                    of disease in the same or another species, if functionality of the gene is \
                    expected to be similar across species.""",
                    TABLE,
                    new AcmgOrigin.Amended(
                            "PVS1",
                            "loss-of-function evidence may come from another species where the"
                                    + " gene's function is expected to be similar"));

    public static final Criterion PS1 =
            new Criterion(
                    "PS1",
                    Direction.PATHOGENIC,
                    Weight.STRONG,
                    """
                    Same amino acid change as a previously established pathogenic variant \
                    regardless of nucleotide change.""",
                    TABLE,
                    new AcmgOrigin.Retained("PS1"));

    /**
     * Reworded, and the rewording is why ACMG's {@code PM6} no longer exists: AVCG asks for
     * unaffected parental samples that tested negative, where ACMG asked for confirmed parentage
     * and no family history. "Assumed de novo" stopped being a separate, weaker criterion because
     * this one now states what counts as evidence.
     */
    public static final Criterion PS2 =
            new Criterion(
                    "PS2",
                    Direction.PATHOGENIC,
                    Weight.STRONG,
                    """
                    de novo in a patient with the disease and unaffected parental samples tested \
                    negative.""",
                    TABLE,
                    new AcmgOrigin.Amended(
                            "PS2",
                            "requires unaffected parental samples tested negative, where ACMG/AMP"
                                    + " required confirmed maternity and paternity and no family"
                                    + " history"));

    public static final Criterion PS3 =
            new Criterion(
                    "PS3",
                    Direction.PATHOGENIC,
                    Weight.STRONG,
                    """
                    Well-established in vitro or in vivo functional studies supportive of a \
                    damaging effect on the gene or gene product.""",
                    TABLE,
                    new AcmgOrigin.Retained("PS3"));

    public static final Criterion PS4 =
            new Criterion(
                    "PS4",
                    Direction.PATHOGENIC,
                    Weight.STRONG,
                    """
                    The prevalence of the variant in affected individuals is significantly \
                    increased compared with the prevalence in controls.""",
                    TABLE,
                    new AcmgOrigin.Retained("PS4"));

    /**
     * The renaming that catches people out. This is ACMG's {@code PP1}, promoted from supporting to
     * strong — so anyone reading it as "the supporting cosegregation criterion" has both the code
     * and the weight wrong.
     */
    public static final Criterion PS5 =
            new Criterion(
                    "PS5",
                    Direction.PATHOGENIC,
                    Weight.STRONG,
                    """
                    Cosegregation with disease in multiple affected family members in a gene \
                    definitively known to cause the disease.""",
                    TABLE,
                    new AcmgOrigin.Renumbered("PP1", "reweighted from supporting to strong"));

    /**
     * Amended where it matters for animals: ACMG asks for a domain without benign variation, AVCG
     * asks for one without benign variation <em>across breeds and/or species</em>. Breed structure
     * is exactly what makes an animal population different from a human one.
     */
    public static final Criterion PM1 =
            new Criterion(
                    "PM1",
                    Direction.PATHOGENIC,
                    Weight.MODERATE,
                    """
                    Located in a mutational hot-spot and/or critical and well-established \
                    functional domain (e.g., active site of an enzyme) without benign variation \
                    across breeds and/or species.""",
                    TABLE,
                    new AcmgOrigin.Amended(
                            "PM1", "benign variation is assessed across breeds and/or species"));

    /**
     * ACMG's {@code PM5}, renumbered because ACMG's own {@code PM2} — an allele-frequency
     * criterion — was removed. Animal populations rarely have the variant databases that criterion
     * assumed.
     */
    public static final Criterion PM2 =
            new Criterion(
                    "PM2",
                    Direction.PATHOGENIC,
                    Weight.MODERATE,
                    """
                    Novel missense change at an amino acid residue where a different missense \
                    change has been determined to be pathogenic in other individuals.""",
                    TABLE,
                    new AcmgOrigin.Renumbered(
                            "PM5",
                            "renumbered when ACMG/AMP PM2, an allele-frequency criterion, was"
                                    + " removed"));

    public static final Criterion PM3 =
            new Criterion(
                    "PM3",
                    Direction.PATHOGENIC,
                    Weight.MODERATE,
                    """
                    For recessive disorders, detected in trans with a pathogenic variant.""",
                    TABLE,
                    new AcmgOrigin.Retained("PM3"));

    public static final Criterion PM4 =
            new Criterion(
                    "PM4",
                    Direction.PATHOGENIC,
                    Weight.MODERATE,
                    """
                    Protein length changes as a result of in-frame deletions/insertions in a \
                    non-repetitive region or stop-loss variants.""",
                    TABLE,
                    new AcmgOrigin.Retained("PM4"));

    /**
     * New in AVCG, and one of only two additions. The pathogenic half of using other species'
     * evidence; {@link #BP6} is not its opposite — {@code BP1} is.
     */
    public static final Criterion PP1 =
            new Criterion(
                    "PP1",
                    Direction.PATHOGENIC,
                    Weight.SUPPORTIVE,
                    """
                    Cross-species alignment shows the variant is conserved and other information \
                    across species (e.g., ClinVar data) states the variant is pathogenic.""",
                    TABLE,
                    new AcmgOrigin.NewInAvcg(
                            "integrates conservation and clinical data from other species"));

    public static final Criterion PP2 =
            new Criterion(
                    "PP2",
                    Direction.PATHOGENIC,
                    Weight.SUPPORTIVE,
                    """
                    Missense variant in a gene that has a low rate of benign missense variation and \
                    in which missense variants are a common mechanism of disease.""",
                    TABLE,
                    new AcmgOrigin.Retained("PP2"));

    /**
     * Stricter than ACMG's, and the difference is easy to miss: ACMG accepts <em>multiple lines</em>
     * of computational evidence, AVCG requires <em>all</em> of it to agree. AVCG also names which
     * tool combinations count and treats a disagreement between them as no evidence at all — that
     * belongs to the predictor adapters, not here, but it is the same tightening.
     */
    public static final Criterion PP3 =
            new Criterion(
                    "PP3",
                    Direction.PATHOGENIC,
                    Weight.SUPPORTIVE,
                    """
                    All computational evidence supports a deleterious effect on the gene or gene \
                    product (conservation, evolutionary, splicing impact, etc.).""",
                    TABLE,
                    new AcmgOrigin.Amended(
                            "PP3",
                            "requires all computational evidence to agree, where ACMG/AMP required"
                                    + " multiple lines of it"));

    public static final Criterion PP4 =
            new Criterion(
                    "PP4",
                    Direction.PATHOGENIC,
                    Weight.SUPPORTIVE,
                    """
                    Patient's phenotype or family history is highly specific for a disease with a \
                    single genetic etiology.""",
                    TABLE,
                    new AcmgOrigin.Retained("PP4"));

    /**
     * ACMG's {@code BS4}, renumbered because ACMG's own {@code BS1} — allele frequency greater than
     * expected for the disorder — was removed. Both benign frequency criteria went: breed
     * structure, founder effects and popular sires make animal allele frequencies mean something
     * different, and the population databases the human criteria assumed largely do not exist.
     */
    public static final Criterion BS1 =
            new Criterion(
                    "BS1",
                    Direction.BENIGN,
                    Weight.STRONG,
                    """
                    Lack of segregation in affected members of a family.""",
                    TABLE,
                    new AcmgOrigin.Renumbered(
                            "BS4",
                            "renumbered when ACMG/AMP BS1 and BA1, both allele-frequency criteria,"
                                    + " were removed"));

    public static final Criterion BS2 =
            new Criterion(
                    "BS2",
                    Direction.BENIGN,
                    Weight.STRONG,
                    """
                    Observed in a healthy adult individual for a recessive (homozygous), dominant \
                    (heterozygous), or X-linked (hemizygous) disorder, with full penetrance \
                    expected at an early age.""",
                    TABLE,
                    new AcmgOrigin.Retained("BS2"));

    public static final Criterion BS3 =
            new Criterion(
                    "BS3",
                    Direction.BENIGN,
                    Weight.STRONG,
                    """
                    Well-established in vitro or in vivo functional studies show no damaging effect \
                    on protein function or splicing.""",
                    TABLE,
                    new AcmgOrigin.Retained("BS3"));

    /**
     * New in AVCG, and the second of the two additions. The benign counterpart of {@link #PP1} —
     * and the reason AVCG's {@code BP1} must never be read as ACMG's, which was a missense
     * criterion removed for being too restrictive without evidence.
     */
    public static final Criterion BP1 =
            new Criterion(
                    "BP1",
                    Direction.BENIGN,
                    Weight.SUPPORTIVE,
                    """
                    Cross-species alignment shows the variant is not conserved and other \
                    information across species (e.g., ClinVar data) states the variant is benign.""",
                    TABLE,
                    new AcmgOrigin.NewInAvcg(
                            "integrates conservation and clinical data from other species; ACMG/AMP"
                                    + " BP1 was a different criterion and was removed"));

    public static final Criterion BP2 =
            new Criterion(
                    "BP2",
                    Direction.BENIGN,
                    Weight.SUPPORTIVE,
                    """
                    Observed in trans with a pathogenic variant for a fully penetrant dominant \
                    gene/disorder or observed in cis with a pathogenic variant in any inheritance \
                    pattern.""",
                    TABLE,
                    new AcmgOrigin.Retained("BP2"));

    public static final Criterion BP3 =
            new Criterion(
                    "BP3",
                    Direction.BENIGN,
                    Weight.SUPPORTIVE,
                    """
                    In-frame deletions/insertions in a repetitive region without a known \
                    function.""",
                    TABLE,
                    new AcmgOrigin.Retained("BP3"));

    /** The benign mirror of {@link #PP3}'s tightening: all the evidence, not multiple lines of it. */
    public static final Criterion BP4 =
            new Criterion(
                    "BP4",
                    Direction.BENIGN,
                    Weight.SUPPORTIVE,
                    """
                    All computational evidence supports a benign effect on the gene or gene product \
                    (conservation, evolutionary, splicing impact, etc.).""",
                    TABLE,
                    new AcmgOrigin.Amended(
                            "BP4",
                            "requires all computational evidence to agree, where ACMG/AMP required"
                                    + " multiple lines of it suggesting no impact"));

    public static final Criterion BP5 =
            new Criterion(
                    "BP5",
                    Direction.BENIGN,
                    Weight.SUPPORTIVE,
                    """
                    Variant found in a case with an alternate molecular basis for disease.""",
                    TABLE,
                    new AcmgOrigin.Retained("BP5"));

    /** ACMG's {@code BP7}, renumbered when ACMG's own {@code BP6} was removed. */
    public static final Criterion BP6 =
            new Criterion(
                    "BP6",
                    Direction.BENIGN,
                    Weight.SUPPORTIVE,
                    """
                    A synonymous (silent) variant for which splicing prediction algorithms predict \
                    no impact to the splice consensus sequence nor the creation of a new splice \
                    site AND the nucleotide is not highly conserved.""",
                    TABLE,
                    new AcmgOrigin.Renumbered(
                            "BP7",
                            "renumbered to close the gap left by removing ACMG/AMP BP6, an"
                                    + " unreviewable reputable-source assertion"));

    /**
     * Inventory order: pathogenic before benign, and within each, descending weight then number.
     * This is the order Table 4 uses and the order the generated reference emits. Fixed, because
     * the Definition of Done requires identical bytes from identical input, and a map's iteration
     * order is not a promise.
     */
    private static final List<Criterion> ALL =
            List.of(
                    PVS1, PS1, PS2, PS3, PS4, PS5, PM1, PM2, PM3, PM4, PP1, PP2, PP3, PP4,
                    BS1, BS2, BS3, BP1, BP2, BP3, BP4, BP5, BP6);

    private static final Map<String, Criterion> BY_CODE = indexByCode(ALL);

    private static Map<String, Criterion> indexByCode(List<Criterion> criteria) {
        Map<String, Criterion> index = new LinkedHashMap<>();
        for (Criterion criterion : criteria) {
            Criterion clash = index.put(criterion.code(), criterion);
            if (clash != null) {
                throw new IllegalStateException("duplicate criterion code: " + criterion.code());
            }
        }
        return Map.copyOf(index);
    }

    /** The edition these criteria come from, for anything that records provenance. */
    public static GuidelineEdition edition() {
        return GuidelineEdition.AVCG_2024;
    }

    /** Every criterion, in inventory order. */
    public static List<Criterion> all() {
        return ALL;
    }

    /**
     * An unknown code yields an empty result rather than an exception: a user typing {@code PS9} is
     * asking a question, not causing a failure, and the caller is better placed to say so.
     */
    public static Optional<Criterion> byCode(String code) {
        return Optional.ofNullable(code).map(BY_CODE::get);
    }

    private Avcg2024() {}
}
