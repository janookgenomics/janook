package com.janookgenomics.janook.core.boundary;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The bytecode half of the core boundary, defined once so that the rule which guards the real code
 * and the rule proven by the tripwire are the same object.
 *
 * <p>Reading bytecode rather than source is deliberate. A source scan sees {@code import} statements
 * and misses reachability through a superclass, an interface, a field type or a returned value —
 * exactly the routes by which I/O arrives without anyone typing an import.
 */
final class CoreBoundaryRules {

    /** Package where the deliberately-violating fixtures live. */
    static final String FIXTURES = CoreBoundaryRules.class.getPackageName() + ".fixtures";

    private static final String WHY =
            "janook-core is a pure function from evidence to classification. Reading files, opening "
                    + "sockets or talking to a database is the outer modules' work: janook-cli owns "
                    + "input, janook-store owns persistence. A core that does its own I/O cannot be "
                    + "embedded in someone else's pipeline, cannot be tested without a filesystem, "
                    + "and quietly acquires the dependencies this module exists to refuse. If core "
                    + "appears to need I/O, invert it: define the interface here and let an outer "
                    + "module implement it";

    static final ArchRule NO_IO =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("java.io..", "java.nio.file..", "java.net..", "java.sql..")
                    .because(WHY);

    /** Core's production classes only — test sources are not part of what an embedder receives. */
    static JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.janookgenomics.janook.core");
    }

    static JavaClasses fixtureClasses() {
        return new ClassFileImporter().importPackages(FIXTURES);
    }

    private CoreBoundaryRules() {}
}
