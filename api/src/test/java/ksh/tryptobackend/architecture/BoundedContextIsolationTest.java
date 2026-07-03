package ksh.tryptobackend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
        packages = "ksh.tryptobackend",
        importOptions = ImportOption.DoNotIncludeTests.class)
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
    void investmentround_isolation(JavaClasses classes) {
        assertContextIsolation("investmentround", classes);
    }

    @ArchTest
    void marketdata_isolation(JavaClasses classes) {
        assertContextIsolation("marketdata", classes);
    }

    @ArchTest
    void common_should_not_depend_on_any_context(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage(COMMON + "..")
                .and()
                .resideOutsideOfPackage(COMMON + ".seed..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(allContextRootPackages())
                .as("Common should not depend on any bounded context (seed excluded)")
                .check(classes);
    }

    private void assertContextIsolation(String context, JavaClasses classes) {
        String[] otherPortInPackages = otherContextPortInPackages(context);
        String[] otherPortOutPackages = otherContextPortOutPackages(context);

        for (String other : BOUNDED_CONTEXTS) {
            if (other.equals(context)) continue;

            noClasses()
                    .that()
                    .resideInAPackage(contextPkg(context, ".."))
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(forbiddenPackagesOf(other))
                    .as(context + " should not access forbidden packages of " + other)
                    .check(classes);
        }

        noClasses()
                .that()
                .resideInAPackage(contextPkg(context, ".."))
                .and()
                .resideOutsideOfPackage(contextPkg(context, ".adapter.out.acl.."))
                .and()
                .resideOutsideOfPackage(contextPkg(context, ".adapter.out.service.."))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(otherPortInPackages)
                .as(
                        context
                                + " — only adapter.out.acl/service may depend on other context"
                                + " UseCases")
                .check(classes);

        FreezingArchRule.freeze(
                        noClasses()
                                .that()
                                .resideInAPackage(contextPkg(context, SERVICE))
                                .should()
                                .dependOnClassesThat()
                                .resideInAnyPackage(otherPortOutPackages)
                                .as(
                                        context
                                                + " service should not depend on other context's"
                                                + " OutputPort — cross-context goes through"
                                                + " UseCase"))
                .check(classes);
    }
}
