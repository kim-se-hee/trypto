package ksh.tryptobackend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

@AnalyzeClasses(packages = "ksh.tryptobackend", importOptions = ImportOption.DoNotIncludeTests.class)
class BoundedContextIsolationTest {

    @ArchTest
    void trading_isolation(JavaClasses classes) {
        assertContextIsolation("trading", classes);
    }

    @ArchTest
    void wallet_isolation(JavaClasses classes) {
        assertContextIsolation("wallet", classes);
    }

    @ArchTest
    void transfer_isolation(JavaClasses classes) {
        assertContextIsolation("transfer", classes);
    }

    @ArchTest
    void investmentround_isolation(JavaClasses classes) {
        assertContextIsolation("investmentround", classes);
    }

    @ArchTest
    void ranking_isolation(JavaClasses classes) {
        assertContextIsolation("ranking", classes);
    }

    @ArchTest
    void regretanalysis_isolation(JavaClasses classes) {
        assertContextIsolation("regretanalysis", classes);
    }

    @ArchTest
    void portfolio_isolation(JavaClasses classes) {
        assertContextIsolation("portfolio", classes);
    }

    @ArchTest
    void marketdata_isolation(JavaClasses classes) {
        assertContextIsolation("marketdata", classes);
    }

    @ArchTest
    void common_should_not_depend_on_any_context(JavaClasses classes) {
        noClasses()
            .that().resideInAPackage(COMMON + "..")
            .and().resideOutsideOfPackage(COMMON + ".seed..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(allContextRootPackages())
            .as("Common should not depend on any bounded context (seed excluded)")
            .check(classes);
    }

    @ArchTest
    void batch_should_only_depend_on_context_use_cases(JavaClasses classes) {
        noClasses()
            .that().resideInAPackage(BATCH + "..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(allContextForbiddenPackages())
            .as("Batch should only depend on UseCase (port.in) of bounded contexts")
            .check(classes);
    }

    private void assertContextIsolation(String context, JavaClasses classes) {
        String[] otherPortInPackages = otherContextPortInPackages(context);

        for (String other : BOUNDED_CONTEXTS) {
            if (other.equals(context)) continue;

            noClasses()
                .that().resideInAPackage(contextPkg(context, ".."))
                .should().dependOnClassesThat()
                .resideInAnyPackage(forbiddenPackagesOf(other))
                .as(context + " should not access forbidden packages of " + other)
                .check(classes);
        }

        noClasses()
            .that().resideInAPackage(contextPkg(context, ".."))
            .and().resideOutsideOfPackage(contextPkg(context, SERVICE))
            .should().dependOnClassesThat()
            .resideInAnyPackage(otherPortInPackages)
            .as(context + " non-service classes should not depend on other context UseCases")
            .check(classes);
    }
}
