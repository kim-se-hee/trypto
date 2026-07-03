package ksh.tryptobackend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "ksh.tryptobackend",
        importOptions = ImportOption.DoNotIncludeTests.class)
class PackagePlacementTest {

    @ArchTest
    void usecases_should_reside_in_port_in(JavaClasses classes) {
        classes()
                .that()
                .haveSimpleNameEndingWith("UseCase")
                .and()
                .areInterfaces()
                .should()
                .resideInAnyPackage(allContextDirectPackages(PORT_IN))
                .as("UseCase interfaces should reside in application.port.in")
                .check(classes);
    }

    @ArchTest
    void services_should_reside_in_service_package(JavaClasses classes) {
        classes()
                .that()
                .haveSimpleNameEndingWith("Service")
                .and()
                .resideInAnyPackage(allContextPackages(".."))
                .and()
                .areNotInterfaces()
                .should()
                .resideInAnyPackage(
                        merge(allContextPackages(SERVICE), allContextPackages(DOMAIN_SERVICE)))
                .as(
                        "Service implementations should reside in application.service or"
                                + " domain.service")
                .check(classes);
    }

    @ArchTest
    void controllers_should_reside_in_adapter_in(JavaClasses classes) {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .resideInAnyPackage(allContextPackages(".adapter.in.web.."))
                .as("Controllers should reside in adapter.in.web")
                .check(classes);
    }

    @ArchTest
    void entities_should_reside_in_adapter_out_entity(JavaClasses classes) {
        classes()
                .that()
                .areAnnotatedWith(Entity.class)
                .should()
                .resideInAnyPackage(allContextPackages(".adapter.out.persistence.entity.."))
                .as("JPA entities should reside in adapter.out.persistence.entity")
                .check(classes);
    }

    @ArchTest
    void repositories_should_reside_in_adapter_out_repository(JavaClasses classes) {
        classes()
                .that()
                .areAssignableTo(JpaRepository.class)
                .should()
                .resideInAnyPackage(allContextPackages(".adapter.out.persistence.repository.."))
                .as("JPA repositories should reside in adapter.out.persistence.repository")
                .check(classes);
    }

    @ArchTest
    void ports_should_reside_in_port_out(JavaClasses classes) {
        classes()
                .that()
                .haveSimpleNameEndingWith("Port")
                .and()
                .areInterfaces()
                .should()
                .resideInAnyPackage(allContextDirectPackages(PORT_OUT))
                .as("Port interfaces should reside in application.port.out")
                .check(classes);
    }

    @ArchTest
    void no_classes_should_reside_directly_in_domain_root(JavaClasses classes) {
        noClasses()
                .should()
                .resideInAnyPackage(allContextDirectPackages(".domain"))
                .as("Domain classes should reside in domain.model or domain.vo, not in domain root")
                .check(classes);
    }

    @ArchTest
    void events_should_reside_in_domain_event(JavaClasses classes) {
        classes()
                .that()
                .haveSimpleNameEndingWith("Event")
                .and()
                .resideInAnyPackage(allContextPackages(".."))
                .should()
                .resideInAnyPackage(allContextPackages(".domain.event.."))
                .as("Domain events should reside in domain.event")
                .check(classes);
    }

    @ArchTest
    void domain_service_impls_should_reside_in_adapter_out_service(JavaClasses classes) {
        classes()
                .that(implementDomainServiceInterface())
                .should()
                .resideInAnyPackage(allContextPackages(".adapter.out.service.."))
                .as("Domain service implementations should reside in adapter.out.service")
                .check(classes);
    }

    private static DescribedPredicate<JavaClass> implementDomainServiceInterface() {
        return new DescribedPredicate<>("implement an interface residing in domain.service") {
            @Override
            public boolean test(JavaClass javaClass) {
                if (javaClass.isInterface()) {
                    return false;
                }
                return javaClass.getAllRawInterfaces().stream()
                        .anyMatch(
                                i ->
                                        i.getPackageName().startsWith(BASE)
                                                && i.getPackageName().contains(".domain.service"));
            }
        };
    }
}
