package ksh.tryptobackend.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@AnalyzeClasses(
        packages = "ksh.tryptobackend",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalLayerDependencyTest {

    @ArchTest
    void domain_should_not_depend_on_application_or_adapter_or_framework(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAnyPackage(allContextPackages(DOMAIN))
                .and()
                .resideOutsideOfPackage("..domain.service..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        merge(
                                allContextPackages(SERVICE),
                                allContextPackages(".application.port.out.."),
                                allContextPackages(".application.port.in"),
                                allContextPackages(STRATEGY),
                                allContextPackages(ADAPTER),
                                new String[] {
                                    "org.springframework..",
                                    "jakarta.persistence..",
                                    "jakarta.transaction..",
                                    "com.querydsl.."
                                }))
                .as(
                        "Domain should not depend on application (except port.in.dto), adapter, or"
                                + " framework")
                .check(classes);
    }

    @ArchTest
    void application_should_not_depend_on_adapter(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAnyPackage(allContextPackages(APPLICATION))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(allContextPackages(ADAPTER))
                .as("Application should not depend on adapter")
                .check(classes);
    }

    @ArchTest
    void application_service_should_not_depend_on_usecase(JavaClasses classes) {
        DescribedPredicate<JavaClass> aUseCase =
                describe("a UseCase", jc -> jc.getSimpleName().endsWith("UseCase"));
        noFields()
                .that()
                .areDeclaredInClassesThat()
                .resideInAnyPackage(allContextPackages(SERVICE))
                .should()
                .haveRawType(aUseCase)
                .as("Application services must not inject another UseCase — collaborate via ports")
                .check(classes);
    }

    @ArchTest
    void application_service_should_not_depend_on_service(JavaClasses classes) {
        DescribedPredicate<JavaClass> anApplicationService =
                describe(
                        "an application service",
                        jc ->
                                jc.getSimpleName().endsWith("Service")
                                        && jc.getPackageName().contains(".application.service"));
        noFields()
                .that()
                .areDeclaredInClassesThat()
                .resideInAnyPackage(allContextPackages(SERVICE))
                .should()
                .haveRawType(anApplicationService)
                .as(
                        "Application services must not inject another application service —"
                                + " collaborate via ports or domain services")
                .check(classes);
    }

    @ArchTest
    void adapter_in_should_not_depend_on_adapter_out(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAnyPackage(allContextPackages(ADAPTER_IN))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(allContextPackages(ADAPTER_OUT))
                .as("Adapter in should not depend on adapter out")
                .check(classes);
    }

    @ArchTest
    void adapter_in_should_not_depend_on_port_out(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAnyPackage(allContextPackages(ADAPTER_IN))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(allContextPackages(".application.port.out.."))
                .as("Adapter in should not depend on port out")
                .check(classes);
    }

    @ArchTest
    void adapter_in_should_not_depend_on_application_service(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAnyPackage(allContextPackages(ADAPTER_IN))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(allContextPackages(SERVICE))
                .as("Adapter in should depend on UseCase (port.in), not Service implementations")
                .check(classes);
    }

    @ArchTest
    void adapter_out_should_not_depend_on_adapter_in(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAnyPackage(allContextPackages(ADAPTER_OUT))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(allContextPackages(ADAPTER_IN))
                .as("Adapter out should not depend on adapter in")
                .check(classes);
    }

    @ArchTest
    void output_port_should_not_depend_on_port_out_dto(JavaClasses classes) {
        FreezingArchRule.freeze(
                        noClasses()
                                .that()
                                .resideInAnyPackage(allContextPackages(PORT_OUT))
                                .should()
                                .dependOnClassesThat()
                                .resideInAnyPackage(
                                        allContextPackages(".application.port.out.dto.."))
                                .as(
                                        "Output Port should not depend on port.out.dto — return"
                                                + " domain model/VO instead"))
                .check(classes);
    }

    @ArchTest
    void service_should_not_depend_on_port_out_dto(JavaClasses classes) {
        FreezingArchRule.freeze(
                        noClasses()
                                .that()
                                .resideInAnyPackage(allContextPackages(SERVICE))
                                .should()
                                .dependOnClassesThat()
                                .resideInAnyPackage(
                                        allContextPackages(".application.port.out.dto.."))
                                .as(
                                        "Service should not depend on port.out.dto — use domain"
                                                + " model/VO instead"))
                .check(classes);
    }

    @ArchTest
    void application_and_domain_should_not_depend_on_adapter_dto(JavaClasses classes) {
        FreezingArchRule.freeze(
                        noClasses()
                                .that()
                                .resideInAnyPackage(
                                        merge(
                                                allContextPackages(APPLICATION),
                                                allContextPackages(DOMAIN)))
                                .should()
                                .dependOnClassesThat()
                                .resideInAnyPackage(
                                        merge(
                                                allContextPackages(".adapter.in.dto.request.."),
                                                allContextPackages(".adapter.in.dto.response..")))
                                .as(
                                        "Application and domain should not depend on adapter"
                                                + " Request/Response DTOs"))
                .check(classes);
    }

    @ArchTest
    void domain_vo_should_not_depend_on_domain_model(JavaClasses classes) {
        FreezingArchRule.freeze(
                        noClasses()
                                .that()
                                .resideInAnyPackage(allContextPackages(".domain.vo.."))
                                .should()
                                .dependOnClassesThat()
                                .resideInAnyPackage(allContextPackages(".domain.model.."))
                                .as(
                                        "Domain VO should not depend on domain model — pass values"
                                                + " into VO methods instead of aggregates"))
                .check(classes);
    }
}
