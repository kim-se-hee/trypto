package ksh.tryptobackend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

@AnalyzeClasses(packages = "ksh.tryptobackend", importOptions = ImportOption.DoNotIncludeTests.class)
class NamingConventionTest {

    @ArchTest
    void usecases_should_end_with_UseCase(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextDirectPackages(PORT_IN))
            .and().areInterfaces()
            .should().haveSimpleNameEndingWith("UseCase")
            .as("UseCase interfaces should end with 'UseCase'")
            .check(classes);
    }

    @ArchTest
    void output_ports_should_end_with_Port(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextDirectPackages(PORT_OUT))
            .and().areInterfaces()
            .should().haveSimpleNameEndingWith("Port")
            .as("Output Port interfaces should end with 'Port'")
            .check(classes);
    }

    @ArchTest
    void services_should_end_with_Service(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(SERVICE))
            .should().haveSimpleNameEndingWith("Service")
            .as("Service classes should end with 'Service'")
            .check(classes);
    }

    @ArchTest
    void controllers_should_end_with_Controller(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextDirectPackages(".adapter.in"))
            .and().areAnnotatedWith(RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .as("Controller classes should end with 'Controller'")
            .check(classes);
    }

    @ArchTest
    void adapters_should_end_with_Adapter(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextDirectPackages(".adapter.out"))
            .and().areNotInterfaces()
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Adapter")
            .as("Adapter classes should end with 'Adapter'")
            .check(classes);
    }

    @ArchTest
    void jpa_entities_should_end_with_JpaEntity(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".adapter.out.entity.."))
            .and().areNotAnnotatedWith(jakarta.persistence.Embeddable.class)
            .and().areTopLevelClasses()
            .and().haveSimpleNameNotStartingWith("Q")
            .should().haveSimpleNameEndingWith("JpaEntity")
            .as("JPA entity classes should end with 'JpaEntity'")
            .check(classes);
    }

    @ArchTest
    void repositories_should_end_with_JpaRepository(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".adapter.out.repository.."))
            .should().haveSimpleNameEndingWith("JpaRepository")
            .as("Repository interfaces should end with 'JpaRepository'")
            .check(classes);
    }

    @ArchTest
    void commands_should_end_with_Command(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".application.port.in.dto.command.."))
            .should().haveSimpleNameEndingWith("Command")
            .as("Command DTOs should end with 'Command'")
            .check(classes);
    }

    @ArchTest
    void queries_should_end_with_Query(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".application.port.in.dto.query.."))
            .should().haveSimpleNameEndingWith("Query")
            .as("Query DTOs should end with 'Query'")
            .check(classes);
    }

    @ArchTest
    void results_should_end_with_Result(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".application.port.in.dto.result.."))
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Result")
            .as("Result DTOs should end with 'Result'")
            .check(classes);
    }

    @ArchTest
    void requests_should_end_with_Request(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".adapter.in.dto.request.."))
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Request")
            .as("Request DTOs should end with 'Request'")
            .check(classes);
    }

    @ArchTest
    void responses_should_end_with_Response(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".adapter.in.dto.response.."))
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Response")
            .as("Response DTOs should end with 'Response'")
            .check(classes);
    }
}
