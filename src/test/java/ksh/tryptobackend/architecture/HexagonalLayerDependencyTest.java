package ksh.tryptobackend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

@AnalyzeClasses(packages = "ksh.tryptobackend", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalLayerDependencyTest {

    @ArchTest
    void domain_should_not_depend_on_application_or_adapter_or_framework(JavaClasses classes) {
        noClasses()
            .that().resideInAnyPackage(allContextPackages(DOMAIN))
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                merge(
                    allContextPackages(APPLICATION),
                    allContextPackages(ADAPTER),
                    new String[]{
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.transaction..",
                        "com.querydsl.."
                    }
                )
            )
            .as("Domain should not depend on application, adapter, or framework")
            .check(classes);
    }

    @ArchTest
    void application_should_not_depend_on_adapter(JavaClasses classes) {
        noClasses()
            .that().resideInAnyPackage(allContextPackages(APPLICATION))
            .should().dependOnClassesThat()
            .resideInAnyPackage(allContextPackages(ADAPTER))
            .as("Application should not depend on adapter")
            .check(classes);
    }

    @ArchTest
    void adapter_in_should_not_depend_on_adapter_out(JavaClasses classes) {
        noClasses()
            .that().resideInAnyPackage(allContextPackages(ADAPTER_IN))
            .should().dependOnClassesThat()
            .resideInAnyPackage(allContextPackages(ADAPTER_OUT))
            .as("Adapter in should not depend on adapter out")
            .check(classes);
    }

    @ArchTest
    void adapter_out_should_not_depend_on_adapter_in(JavaClasses classes) {
        noClasses()
            .that().resideInAnyPackage(allContextPackages(ADAPTER_OUT))
            .should().dependOnClassesThat()
            .resideInAnyPackage(allContextPackages(ADAPTER_IN))
            .as("Adapter out should not depend on adapter in")
            .check(classes);
    }

    private static String[] merge(String[]... arrays) {
        int total = 0;
        for (String[] arr : arrays) total += arr.length;
        String[] result = new String[total];
        int pos = 0;
        for (String[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }
        return result;
    }
}
