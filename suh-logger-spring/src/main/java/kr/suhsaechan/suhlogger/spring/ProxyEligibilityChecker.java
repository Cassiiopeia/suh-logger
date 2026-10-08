package kr.suhsaechan.suhlogger.spring;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import kr.suhsaechan.suhlogger.internal.spring.AnnotationLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ClassUtils;

/**
 * 프록시가 걸리지 않아 로그가 조용히 빠지는 대상을 기동 시 알린다.
 * Kotlin은 클래스·메서드가 기본 final이라 kotlin-spring(allopen) 플러그인이 열지 않은 곳에 붙인 어노테이션은 동작하지 않는다.
 * 빈 생성 전에 검사해야 final 클래스 때문에 기동이 실패하기 전에 원인을 남길 수 있다.
 */
public class ProxyEligibilityChecker implements BeanFactoryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ProxyEligibilityChecker.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            Class<?> type;
            try {
                type = beanFactory.getType(name, false);
            } catch (RuntimeException e) {
                continue;
            }
            if (type != null) {
                check(ClassUtils.getUserClass(type));
            }
        }
    }

    void check(Class<?> type) {
        boolean classAnnotated = AnnotationLookup.hasAny(type);
        if (classAnnotated && Modifier.isFinal(type.getModifiers())) {
            log.warn("[suh-logger] {} is final, so @LogMonitor/@LogCall/@LogTime cannot be applied "
                    + "(Kotlin: add the kotlin-spring plugin or mark the class open)", type.getName());
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge()) {
                continue;
            }
            boolean annotated = AnnotationLookup.hasAny(method);
            if (!annotated) {
                continue;
            }
            int mod = method.getModifiers();
            if (Modifier.isFinal(type.getModifiers()) && !classAnnotated) {
                log.warn("[suh-logger] {}.{} is in a final class and will not be logged "
                        + "(Kotlin: add the kotlin-spring plugin or mark it open)", type.getName(), method.getName());
            } else if (Modifier.isFinal(mod)) {
                log.warn("[suh-logger] {}.{} is final and will not be logged "
                        + "(Kotlin: mark it open or use the kotlin-spring plugin)", type.getName(), method.getName());
            } else if (Modifier.isPrivate(mod) || Modifier.isStatic(mod)) {
                log.warn("[suh-logger] {}.{} is private or static and will not be logged (Spring AOP proxies only "
                        + "public instance methods)", type.getName(), method.getName());
            }
        }
    }
}
