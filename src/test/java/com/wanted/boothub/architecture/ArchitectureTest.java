package com.wanted.boothub.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 층 경계·도메인 경계 규칙 (/check-boundaries).
 *
 * 패키지는 도메인별(member, lesson, notice ...)이고, 도메인 안에서는 층을 하위 패키지가 아니라
 * 클래스 이름 접미사(*Controller, *Service, *Repository)와 dto 하위 패키지로 나눈다.
 *  - global: 공용 커널. 도메인이 global 을 쓰는 것은 허용, global 이 도메인을 아는 것은 위반
 *  - admin, home: 화면 조립 계층. 다른 도메인의 Service·DTO 호출은 허용, Repository·엔티티 직접 사용은 위반
 */
@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String BASE = "com.wanted.boothub";
    private static final String GLOBAL = BASE + ".global..";
    private static final String[] DOMAINS = {
            BASE + ".member..", BASE + ".lesson..", BASE + ".notice..", BASE + ".admin..", BASE + ".home.."};
    private static final String[] ASSEMBLY = {BASE + ".admin..", BASE + ".home.."};

    /** Controller → Service → Repository 방향만 허용 */
    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(simpleNameEndingWith("Controller"))
            .layer("Service").definedBy(simpleNameEndingWith("Service"))
            .layer("Repository").definedBy(simpleNameEndingWith("Repository"))
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    /** 엔티티는 DTO·Controller·Spring 웹 계층을 모른다 */
    @ArchTest
    static final ArchRule entitiesDoNotKnowDtoOrWeb = noClasses()
            .that().areAnnotatedWith(Entity.class).or().areAnnotatedWith(MappedSuperclass.class)
            .should().dependOnClassesThat(resideInAPackage("..dto..")
                    .or(simpleNameEndingWith("Controller"))
                    .or(resideInAPackage("org.springframework.web..")));

    /** Service 는 다른 도메인의 Repository 를 직접 쓰지 않는다 */
    @ArchTest
    static final ArchRule servicesUseOnlyOwnRepositories = classes()
            .that().haveSimpleNameEndingWith("Service").and().resideInAPackage(BASE + "..")
            .should(notDependOnOtherDomainRepositories());

    /** Policy 는 순수 규칙: Spring 에 의존하지 않는다 */
    @ArchTest
    static final ArchRule policiesArePlainJava = noClasses()
            .that().haveSimpleNameEndingWith("Policy")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    /** 도메인 슬라이스 간 순환 금지 (공용 커널 global 은 제외) */
    @ArchTest
    static final ArchRule domainSlicesAreFreeOfCycles = slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles()
            .ignoreDependency(resideInAPackage(GLOBAL), alwaysTrue())
            .ignoreDependency(alwaysTrue(), resideInAPackage(GLOBAL));

    /** 공용 커널 global 은 도메인을 모른다 */
    @ArchTest
    static final ArchRule kernelDoesNotKnowDomains = noClasses()
            .that().resideInAPackage(GLOBAL)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAINS);

    /** 조립 계층(admin, home)은 다른 도메인의 Repository·엔티티를 직접 다루지 않는다 */
    @ArchTest
    static final ArchRule assemblyUsesServicesOnly = noClasses()
            .that().resideInAnyPackage(ASSEMBLY)
            .should().dependOnClassesThat(simpleNameEndingWith("Repository")
                    .or(CanBeAnnotated.Predicates.annotatedWith(Entity.class)));

    /** 도메인은 조립 계층(admin, home)을 모른다 */
    @ArchTest
    static final ArchRule domainsDoNotKnowAssembly = noClasses()
            .that().resideInAnyPackage(BASE + ".member..", BASE + ".lesson..", BASE + ".notice..", GLOBAL)
            .should().dependOnClassesThat().resideInAnyPackage(ASSEMBLY);

    private static ArchCondition<JavaClass> notDependOnOtherDomainRepositories() {
        return new ArchCondition<>("not depend on repositories of other domains") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                String own = domainOf(service.getPackageName());
                for (Dependency d : service.getDirectDependenciesFromSelf()) {
                    JavaClass target = d.getTargetClass();
                    String other = domainOf(target.getPackageName());
                    if (target.getSimpleName().endsWith("Repository") && other != null && !other.equals(own)) {
                        events.add(SimpleConditionEvent.violated(d, d.getDescription()));
                    }
                }
            }
        };
    }

    private static String domainOf(String packageName) {
        if (!packageName.startsWith(BASE + ".")) {
            return null;
        }
        return packageName.substring(BASE.length() + 1).split("\\.")[0];
    }
}
