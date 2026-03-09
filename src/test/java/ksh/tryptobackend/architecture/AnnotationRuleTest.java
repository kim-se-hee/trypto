package ksh.tryptobackend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

@AnalyzeClasses(packages = "ksh.tryptobackend", importOptions = ImportOption.DoNotIncludeTests.class)
class AnnotationRuleTest {

    @ArchTest
    void domain_models_should_not_use_setter_or_data(JavaClasses classes) {
        noClasses()
            .that().resideInAnyPackage(allContextPackages(".domain.model.."))
            .should().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.Data")
            .as("Domain models should not use @Setter or @Data")
            .check(classes);
    }

    @ArchTest
    void jpa_entities_should_not_use_setter_or_data(JavaClasses classes) {
        noClasses()
            .that().resideInAnyPackage(allContextPackages(".adapter.out.entity.."))
            .should().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.Data")
            .as("JPA entities should not use @Setter or @Data")
            .check(classes);
    }

    @ArchTest
    void no_field_injection_with_autowired(JavaClasses classes) {
        noFields()
            .that().areDeclaredInClassesThat().resideInAPackage(BASE + "..")
            .should().beAnnotatedWith(Autowired.class)
            .as("Field injection with @Autowired is forbidden")
            .check(classes);
    }

    @ArchTest
    void services_should_be_annotated_with_service(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(SERVICE))
            .should().beAnnotatedWith(Service.class)
            .as("Service classes should be annotated with @Service")
            .check(classes);
    }
}
