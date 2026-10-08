package kr.suhsaechan.suhlogger.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 모듈 경계를 사람 대신 빌드가 지킨다. 규칙을 깨는 PR은 리뷰 전에 CI에서 실패한다.
 * 새 환경을 붙일 때는 해당 프레임워크 타입을 어댑터 패키지 안에만 두면 된다.
 */
@AnalyzeClasses(packages = "kr.suhsaechan.suhlogger", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** core(설정·SPI·유틸·내부 처리)는 어떤 프레임워크에도 의존하지 않는다 — 순수 Java·XML·Boot 어디서나 쓰이게 */
    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAnyPackage("..suhlogger.annotation..", "..suhlogger.config..", "..suhlogger.spi..",
                    "..suhlogger.util..", "..suhlogger.internal.mask..", "..suhlogger.internal.http..",
                    "..suhlogger.internal.json..", "..suhlogger.internal.serialize..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "com.fasterxml..",
                    "tools.jackson..", "jakarta..", "javax.servlet..", "reactor..", "org.reactivestreams..");

    /** SPI는 확장 구현자가 보는 계약이라 JDK와 자기 패키지만 본다 */
    @ArchTest
    static final ArchRule spiDependsOnlyOnJdk = classes()
            .that().resideInAPackage("..suhlogger.spi..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..suhlogger.spi..",
                    "..suhlogger.annotation..");

    /** Servlet 타입은 Servlet 어댑터와 자동설정에만 */
    @ArchTest
    static final ArchRule servletStaysInServletAdapter = noClasses()
            .that().resideOutsideOfPackages("..suhlogger.filter..", "..suhlogger.servlet..", "..suhlogger.boot..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.servlet..");

    /** Reactor·WebFlux 타입은 WebFlux 어댑터와 자동설정에만 */
    @ArchTest
    static final ArchRule reactiveStaysInWebfluxAdapter = noClasses()
            .that().resideOutsideOfPackages("..suhlogger.webflux..", "..suhlogger.boot..")
            .should().dependOnClassesThat().resideInAnyPackage("reactor..", "org.springframework.web.server..");

    /** Jackson 타입은 JSON 어댑터와 자동설정에만 — Boot 3(Jackson 2)·4(Jackson 3) 전환이 여기서 끝나게 */
    @ArchTest
    static final ArchRule jacksonStaysInJsonAdapters = noClasses()
            .that().resideOutsideOfPackages("..suhlogger.json..", "..suhlogger.boot..")
            .should().dependOnClassesThat().resideInAnyPackage("com.fasterxml.jackson..", "tools.jackson..");

    /** Spring Boot 타입은 자동설정에만 — 나머지 모듈은 Boot 없이 동작해야 한다 */
    @ArchTest
    static final ArchRule bootStaysInAutoconfigure = noClasses()
            .that().resideOutsideOfPackage("..suhlogger.boot..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.boot..");

    /** aspect는 웹 타입을 직접 모른다 — non-web 앱에서 클래스 로딩이 깨지지 않게 (ResponseEntity는 internal.spring 전담) */
    @ArchTest
    static final ArchRule aspectsDoNotTouchWebTypes = noClasses()
            .that().resideInAPackage("..suhlogger.aspect..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.http..",
                    "org.springframework.web..", "jakarta.servlet..");
}
