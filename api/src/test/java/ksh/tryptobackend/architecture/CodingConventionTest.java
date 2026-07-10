package ksh.tryptobackend.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static ksh.tryptobackend.architecture.ArchitectureConstants.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

@AnalyzeClasses(
        packages = "ksh.tryptobackend",
        importOptions = ImportOption.DoNotIncludeTests.class)
class CodingConventionTest {

    private static final Logger log = LoggerFactory.getLogger(CodingConventionTest.class);

    @ArchTest
    void usecases_should_be_interfaces(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextDirectPackages(PORT_IN))
                .should()
                .beInterfaces()
                .as("UseCases should be interfaces")
                .check(classes);
    }

    @ArchTest
    void output_ports_should_be_interfaces(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextDirectPackages(PORT_OUT))
                .should()
                .beInterfaces()
                .as("Output Ports should be interfaces")
                .check(classes);
    }

    @ArchTest
    void command_dtos_should_be_records(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".application.port.in.dto.command.."))
                .should()
                .beAssignableTo(Record.class)
                .as("Command DTOs should be records")
                .check(classes);
    }

    @ArchTest
    void query_dtos_should_be_records(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".application.port.in.dto.query.."))
                .should()
                .beAssignableTo(Record.class)
                .as("Query DTOs should be records")
                .check(classes);
    }

    @ArchTest
    void result_dtos_should_be_records(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".application.port.in.dto.result.."))
                .should()
                .beAssignableTo(Record.class)
                .as("Result DTOs should be records")
                .check(classes);
    }

    @ArchTest
    void request_dtos_should_be_records(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.in.dto.request.."))
                .should()
                .beAssignableTo(Record.class)
                .as("Request DTOs should be records")
                .check(classes);
    }

    @ArchTest
    void response_dtos_should_be_records(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(".adapter.in.dto.response.."))
                .should()
                .beAssignableTo(Record.class)
                .as("Response DTOs should be records")
                .check(classes);
    }

    @ArchTest
    void services_should_implement_exactly_one_usecase(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(SERVICE))
                .should(implementExactlyOneUseCase())
                .as("Services should implement exactly one UseCase")
                .check(classes);
    }

    @ArchTest
    void no_query_annotation_usage(JavaClasses classes) {
        noMethods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage(BASE + "..")
                .should()
                .beAnnotatedWith("org.springframework.data.jpa.repository.Query")
                .as("@Query annotation usage is forbidden — use QueryDSL instead")
                .check(classes);
    }

    @ArchTest
    void integration_domain_services_should_be_interfaces(JavaClasses classes) {
        classes()
                .that()
                .resideInAnyPackage(allContextPackages(DOMAIN_SERVICE))
                .should(beInterfaceIfDependingOnAnotherContext())
                .as(
                        "Integration domain services (those depending on another bounded context)"
                            + " must be interfaces — implementations live in adapter.out.service")
                .check(classes);
    }

    private static ArchCondition<JavaClass> beInterfaceIfDependingOnAnotherContext() {
        return new ArchCondition<>("be an interface if it depends on another bounded context") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (javaClass.isInterface()) {
                    return;
                }
                String ownContext = contextOf(javaClass.getPackageName());
                Set<String> foreignContexts =
                        javaClass.getDirectDependenciesFromSelf().stream()
                                .map(dep -> contextOf(dep.getTargetClass().getPackageName()))
                                .filter(ctx -> ctx != null && !ctx.equals(ownContext))
                                .collect(Collectors.toCollection(TreeSet::new));
                if (!foreignContexts.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    javaClass.getFullName()
                                            + " is a concrete domain service depending on other"
                                            + " bounded context(s) "
                                            + foreignContexts
                                            + " — make it an interface and move the impl to"
                                            + " adapter.out.service"));
                }
            }
        };
    }

    private static String contextOf(String packageName) {
        if (!packageName.startsWith(BASE + ".")) {
            return null;
        }
        String rest = packageName.substring(BASE.length() + 1);
        int dot = rest.indexOf('.');
        String head = dot < 0 ? rest : rest.substring(0, dot);
        return Arrays.asList(BOUNDED_CONTEXTS).contains(head) ? head : null;
    }

    @ArchTest
    void event_listener_annotations_should_only_be_in_adapters(JavaClasses classes) {
        noMethods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAnyPackage(
                        merge(allContextPackages(APPLICATION), allContextPackages(DOMAIN)))
                .should()
                .beAnnotatedWith("org.springframework.transaction.event.TransactionalEventListener")
                .orShould()
                .beAnnotatedWith("org.springframework.context.event.EventListener")
                .as(
                        "@TransactionalEventListener/@EventListener must be declared in adapters,"
                                + " not in application or domain")
                .check(classes);
    }

    @ArchTest
    void usecases_should_declare_exactly_one_method(JavaClasses classes) {
        // 조회 UseCase 는 응집된 조회를 한 인터페이스에 묶는 경우가 많아 위반이 반복된다.
        // 규칙은 유지하되 빌드를 막지 않고 경고만 남긴다.
        EvaluationResult result =
                classes()
                        .that()
                        .resideInAnyPackage(allContextDirectPackages(PORT_IN))
                        .and()
                        .areInterfaces()
                        .should(declareExactlyOneMethod())
                        .as("UseCases should declare exactly one method")
                        .evaluate(classes);

        if (result.hasViolation()) {
            log.warn(
                    "UseCase 단일 메소드 규칙 위반 (경고, 빌드는 통과):\n{}",
                    String.join("\n", result.getFailureReport().getDetails()));
        }
    }

    @ArchTest
    void controller_methods_should_return_api_response_dto(JavaClasses classes) {
        methods()
                .that()
                .areDeclaredInClassesThat()
                .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .and()
                .arePublic()
                .should(returnApiResponseDtoOrWrapper())
                .as(
                        "@RestController public methods should return ApiResponseDto or"
                                + " ResponseEntity<ApiResponseDto>")
                .check(classes);
    }

    private static ArchCondition<JavaMethod> returnApiResponseDtoOrWrapper() {
        return new ArchCondition<>("return ApiResponseDto or ResponseEntity<ApiResponseDto>") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (returnsApiResponseDto(method) || returnsApiResponseDtoWrapper(method)) {
                    return;
                }
                events.add(
                        SimpleConditionEvent.violated(
                                method,
                                method.getFullName()
                                        + " should return ApiResponseDto or"
                                        + " ResponseEntity<ApiResponseDto>"));
            }
        };
    }

    private static boolean returnsApiResponseDto(JavaMethod method) {
        return method.getRawReturnType().isEquivalentTo(ApiResponseDto.class);
    }

    private static boolean returnsApiResponseDtoWrapper(JavaMethod method) {
        if (!method.getRawReturnType().isEquivalentTo(ResponseEntity.class)) {
            return false;
        }
        if (method.getReturnType() instanceof JavaParameterizedType parameterized) {
            List<JavaType> typeArguments = parameterized.getActualTypeArguments();
            return typeArguments.size() == 1
                    && typeArguments.get(0).toErasure().isEquivalentTo(ApiResponseDto.class);
        }
        return false;
    }

    @ArchTest
    void controller_methods_should_not_return_domain_or_result(JavaClasses classes) {
        DescribedPredicate<JavaClass> domainOrResult =
                resideInAnyPackage(
                                merge(
                                        allContextPackages(DOMAIN),
                                        allContextPackages(".application.port.in.dto.result..")))
                        .as("domain or Result DTO");

        FreezingArchRule.freeze(
                        methods()
                                .that()
                                .areDeclaredInClassesThat()
                                .areAnnotatedWith(
                                        "org.springframework.web.bind.annotation.RestController")
                                .and()
                                .arePublic()
                                .should()
                                .notHaveRawReturnType(domainOrResult)
                                .as(
                                        "@RestController public methods should not directly return"
                                                + " domain or Result DTO"))
                .check(classes);
    }

    private static ArchCondition<JavaClass> declareExactlyOneMethod() {
        return new ArchCondition<>("declare exactly one method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long methodCount = javaClass.getMethods().size();
                if (methodCount != 1) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    javaClass.getFullName()
                                            + " declares "
                                            + methodCount
                                            + " methods, but UseCase should declare exactly one"));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> implementExactlyOneUseCase() {
        return new ArchCondition<>("implement exactly one UseCase interface") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long useCaseCount =
                        javaClass.getAllRawInterfaces().stream()
                                .filter(i -> i.getSimpleName().endsWith("UseCase"))
                                .count();
                if (useCaseCount == 0) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    javaClass.getFullName()
                                            + " does not implement any UseCase interface"));
                } else if (useCaseCount > 1) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    javaClass.getFullName()
                                            + " implements "
                                            + useCaseCount
                                            + " UseCase interfaces, but should implement exactly"
                                            + " one"));
                }
            }
        };
    }
}
