package ksh.tryptobackend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

@AnalyzeClasses(packages = "ksh.tryptobackend", importOptions = ImportOption.DoNotIncludeTests.class)
class PackagePlacementTest {

    @ArchTest
    void usecases_should_reside_in_port_in(JavaClasses classes) {
        classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().areInterfaces()
            .should().resideInAnyPackage(allContextDirectPackages(PORT_IN))
            .as("UseCase interfaces should reside in application.port.in")
            .check(classes);
    }

    @ArchTest
    void services_should_reside_in_service_package(JavaClasses classes) {
        classes()
            .that().haveSimpleNameEndingWith("Service")
            .and().resideInAnyPackage(allContextPackages(".."))
            .and().areNotInterfaces()
            .should().resideInAnyPackage(allContextPackages(SERVICE))
            .as("Service implementations should reside in application.service")
            .check(classes);
    }

    @ArchTest
    void controllers_should_reside_in_adapter_in(JavaClasses classes) {
        classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAnyPackage(allContextDirectPackages(".adapter.in"))
            .as("Controllers should reside in adapter.in")
            .check(classes);
    }

    @ArchTest
    void entities_should_reside_in_adapter_out_entity(JavaClasses classes) {
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAnyPackage(allContextPackages(".adapter.out.entity.."))
            .as("JPA entities should reside in adapter.out.entity")
            .check(classes);
    }

    @ArchTest
    void repositories_should_reside_in_adapter_out_repository(JavaClasses classes) {
        classes()
            .that().areAssignableTo(JpaRepository.class)
            .should().resideInAnyPackage(allContextPackages(".adapter.out.repository.."))
            .as("JPA repositories should reside in adapter.out.repository")
            .check(classes);
    }

    @ArchTest
    void ports_should_reside_in_port_out(JavaClasses classes) {
        classes()
            .that().haveSimpleNameEndingWith("Port")
            .and().areInterfaces()
            .should().resideInAnyPackage(allContextDirectPackages(PORT_OUT))
            .as("Port interfaces should reside in application.port.out")
            .check(classes);
    }

    @ArchTest
    void no_classes_should_reside_directly_in_domain_root(JavaClasses classes) {
        noClasses()
            .should().resideInAnyPackage(allContextDirectPackages(".domain"))
            .as("Domain classes should reside in domain.model or domain.vo, not in domain root")
            .check(classes);
    }
}
