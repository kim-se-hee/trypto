package ksh.tryptobackend.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

@AnalyzeClasses(packages = "ksh.tryptobackend", importOptions = ImportOption.DoNotIncludeTests.class)
class CodingConventionTest {

    @ArchTest
    void usecases_should_be_interfaces(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextDirectPackages(PORT_IN))
            .should().beInterfaces()
            .as("UseCases should be interfaces")
            .check(classes);
    }

    @ArchTest
    void output_ports_should_be_interfaces(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextDirectPackages(PORT_OUT))
            .should().beInterfaces()
            .as("Output Ports should be interfaces")
            .check(classes);
    }

    @ArchTest
    void command_dtos_should_be_records(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".application.port.in.dto.command.."))
            .should().beAssignableTo(Record.class)
            .as("Command DTOs should be records")
            .check(classes);
    }

    @ArchTest
    void query_dtos_should_be_records(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".application.port.in.dto.query.."))
            .should().beAssignableTo(Record.class)
            .as("Query DTOs should be records")
            .check(classes);
    }

    @ArchTest
    void result_dtos_should_be_records(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".application.port.in.dto.result.."))
            .should().beAssignableTo(Record.class)
            .as("Result DTOs should be records")
            .check(classes);
    }

    @ArchTest
    void request_dtos_should_be_records(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".adapter.in.dto.request.."))
            .should().beAssignableTo(Record.class)
            .as("Request DTOs should be records")
            .check(classes);
    }

    @ArchTest
    void response_dtos_should_be_records(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(".adapter.in.dto.response.."))
            .should().beAssignableTo(Record.class)
            .as("Response DTOs should be records")
            .check(classes);
    }

    @ArchTest
    void services_should_implement_exactly_one_usecase(JavaClasses classes) {
        classes()
            .that().resideInAnyPackage(allContextPackages(SERVICE))
            .should(implementExactlyOneUseCase())
            .as("Services should implement exactly one UseCase")
            .check(classes);
    }

    @ArchTest
    void no_query_annotation_usage(JavaClasses classes) {
        noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage(BASE + "..")
            .should().beAnnotatedWith("org.springframework.data.jpa.repository.Query")
            .as("@Query annotation usage is forbidden — use QueryDSL instead")
            .check(classes);
    }

    private static ArchCondition<JavaClass> implementExactlyOneUseCase() {
        return new ArchCondition<>("implement exactly one UseCase interface") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long useCaseCount = javaClass.getAllRawInterfaces().stream()
                    .filter(i -> i.getSimpleName().endsWith("UseCase"))
                    .count();
                if (useCaseCount == 0) {
                    events.add(SimpleConditionEvent.violated(
                        javaClass,
                        javaClass.getFullName() + " does not implement any UseCase interface"));
                } else if (useCaseCount > 1) {
                    events.add(SimpleConditionEvent.violated(
                        javaClass,
                        javaClass.getFullName() + " implements " + useCaseCount
                            + " UseCase interfaces, but should implement exactly one"));
                }
            }
        };
    }
}
