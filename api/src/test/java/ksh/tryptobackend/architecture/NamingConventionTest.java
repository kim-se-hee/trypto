package ksh.tryptobackend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "ksh.tryptobackend", importOptions = ImportOption.DoNotIncludeTests.class)
class NamingConventionTest {

    @ArchTest
    void usecases_should_end_with_UseCase(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextDirectPackages(PORT_IN))
                .and()
                .areInterfaces()
                .should()
                .haveSimpleNameEndingWith("UseCase")
                .as("UseCase interfaces should end with 'UseCase'")
                .check(classes);
    }

    @ArchTest
    void output_ports_should_end_with_Port(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextDirectPackages(PORT_OUT))
                .and()
                .areInterfaces()
                .should()
                .haveSimpleNameEndingWith("Port")
                .as("Output Port interfaces should end with 'Port'")
                .check(classes);
    }

    @ArchTest
    void services_should_end_with_Service(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(SERVICE))
                .should()
                .haveSimpleNameEndingWith("Service")
                .as("Service classes should end with 'Service'")
                .check(classes);
    }

    @ArchTest
    void controllers_should_end_with_Controller(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.in.web.."))
                .and()
                .areAnnotatedWith(RestController.class)
                .should()
                .haveSimpleNameEndingWith("Controller")
                .as("Controller classes should end with 'Controller'")
                .check(classes);
    }

    @ArchTest
    void domain_events_should_end_with_Event(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".domain.event.."))
                .should()
                .haveSimpleNameEndingWith("Event")
                .as("Domain event classes should end with 'Event'")
                .check(classes);
    }

    @ArchTest
    void jpa_entities_should_end_with_JpaEntity(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.out.persistence.entity.."))
                .and()
                .areNotAnnotatedWith(jakarta.persistence.Embeddable.class)
                .and()
                .areTopLevelClasses()
                .and()
                .haveSimpleNameNotStartingWith("Q")
                .should()
                .haveSimpleNameEndingWith("JpaEntity")
                .as("JPA entity classes should end with 'JpaEntity'")
                .check(classes);
    }

    @ArchTest
    void repositories_should_end_with_JpaRepository(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.out.persistence.repository.."))
                .should()
                .haveSimpleNameEndingWith("JpaRepository")
                .as("Repository interfaces should end with 'JpaRepository'")
                .check(classes);
    }

    @ArchTest
    void commands_should_end_with_Command(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".application.port.in.dto.command.."))
                .should()
                .haveSimpleNameEndingWith("Command")
                .as("Command DTOs should end with 'Command'")
                .check(classes);
    }

    @ArchTest
    void queries_should_end_with_Query(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".application.port.in.dto.query.."))
                .should()
                .haveSimpleNameEndingWith("Query")
                .as("Query DTOs should end with 'Query'")
                .check(classes);
    }

    @ArchTest
    void results_should_end_with_Result(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".application.port.in.dto.result.."))
                .and()
                .areTopLevelClasses()
                .should()
                .haveSimpleNameEndingWith("Result")
                .as("Result DTOs should end with 'Result'")
                .check(classes);
    }

    @ArchTest
    void requests_should_end_with_Request(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.in.dto.request.."))
                .and()
                .areTopLevelClasses()
                .should()
                .haveSimpleNameEndingWith("Request")
                .as("Request DTOs should end with 'Request'")
                .check(classes);
    }

    @ArchTest
    void responses_should_end_with_Response(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.in.dto.response.."))
                .and()
                .areTopLevelClasses()
                .should()
                .haveSimpleNameEndingWith("Response")
                .as("Response DTOs should end with 'Response'")
                .check(classes);
    }

    @ArchTest
    void output_ports_should_end_with_QueryPort_or_CommandPort(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextDirectPackages(PORT_OUT))
                .and()
                .areInterfaces()
                .should(endWithQueryPortOrCommandPortOrEventPort())
                .as("Output Port interfaces should end with 'QueryPort', 'CommandPort',"
                        + " 'EventPort', or 'NotificationPort'")
                .check(classes);
    }

    @ArchTest
    void services_should_match_usecase_naming(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(SERVICE))
                .should(matchUseCaseNaming())
                .as("Service name should be UseCase name with 'UseCase' replaced by 'Service'")
                .check(classes);
    }

    @ArchTest
    void persistence_adapters_should_match_port_naming(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.out.persistence.."))
                .and()
                .areNotInterfaces()
                .and()
                .areTopLevelClasses()
                .should(matchPortNamingWithTechPrefix())
                .as("Persistence adapter name should be a tech prefix followed by the Port name"
                        + " with 'Port' replaced by 'Adapter' (e.g. JpaOrderQueryAdapter)")
                .check(classes);
    }

    @ArchTest
    void acl_adapters_should_match_port_naming(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.out.acl.."))
                .and()
                .areNotInterfaces()
                .and()
                .areTopLevelClasses()
                .should(matchAclNamingWithContextPrefix())
                .as("ACL adapter name should be a context prefix followed by 'Acl' and the Port"
                        + " name with 'Port' replaced by 'Adapter' (e.g."
                        + " TradingAclWalletQueryAdapter)")
                .check(classes);
    }

    private static ArchCondition<JavaClass> endWithQueryPortOrCommandPortOrEventPort() {
        return new ArchCondition<>("end with 'QueryPort', 'CommandPort', 'EventPort', or 'NotificationPort'") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String name = javaClass.getSimpleName();
                if (!name.endsWith("QueryPort")
                        && !name.endsWith("CommandPort")
                        && !name.endsWith("EventPort")
                        && !name.endsWith("NotificationPort")) {
                    events.add(SimpleConditionEvent.violated(
                            javaClass,
                            name
                                    + " should end with 'QueryPort', 'CommandPort',"
                                    + " 'EventPort', or 'NotificationPort'"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> matchUseCaseNaming() {
        return new ArchCondition<>("have name matching implemented UseCase") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getAllRawInterfaces().stream()
                        .filter(i -> i.getSimpleName().endsWith("UseCase"))
                        .forEach(useCase -> {
                            String expected = useCase.getSimpleName().replace("UseCase", "Service");
                            if (!javaClass.getSimpleName().equals(expected)) {
                                events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        javaClass.getSimpleName()
                                                + " implements "
                                                + useCase.getSimpleName()
                                                + " but should be named "
                                                + expected));
                            }
                        });
            }
        };
    }

    private static String expectedAdapterBase(JavaClass port) {
        String portName = port.getSimpleName();
        return portName.substring(0, portName.length() - "Port".length()) + "Adapter";
    }

    private static ArchCondition<JavaClass> matchPortNamingWithTechPrefix() {
        return new ArchCondition<>("have a tech prefix followed by the Port name with 'Port' replaced by 'Adapter'") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getAllRawInterfaces().stream()
                        .filter(i -> i.getSimpleName().endsWith("Port"))
                        .forEach(port -> {
                            String base = expectedAdapterBase(port);
                            String actual = javaClass.getSimpleName();
                            if (!actual.endsWith(base) || actual.length() <= base.length()) {
                                events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        actual
                                                + " implements "
                                                + port.getSimpleName()
                                                + " but should be named {tech}"
                                                + base));
                            }
                        });
            }
        };
    }

    private static ArchCondition<JavaClass> matchAclNamingWithContextPrefix() {
        return new ArchCondition<>(
                "have a context prefix followed by 'Acl' and the Port name with 'Port' replaced by" + " 'Adapter'") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getAllRawInterfaces().stream()
                        .filter(i -> i.getSimpleName().endsWith("Port"))
                        .forEach(port -> {
                            String suffix = "Acl" + expectedAdapterBase(port);
                            String actual = javaClass.getSimpleName();
                            if (!actual.endsWith(suffix) || actual.length() <= suffix.length()) {
                                events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        actual
                                                + " implements "
                                                + port.getSimpleName()
                                                + " but should be named {context}"
                                                + suffix));
                            }
                        });
            }
        };
    }
}
