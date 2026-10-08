package kr.suhsaechan.suhlogger.internal.mask;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.internal.serialize.TypeHandlers;
import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import kr.suhsaechan.suhlogger.util.SuhLogger;

/**
 * 객체를 Map·List·값 트리로 바꾼다 — 필드 단위 마스킹과 읽을 수 있는 출력을 위해.
 * 2.x는 DTO를 toString()으로 찍어 record·Lombok @Data의 토큰이 그대로 노출됐다.
 * 앱의 JsonCodec(Jackson)을 먼저 쓰고, 없거나 실패하면 리플렉션으로 필드를 읽는다.
 */
public final class ObjectTrees {

    private static final int MAX_DEPTH = 8;

    private ObjectTrees() {
    }

    public static Object toTree(Object value) {
        return convert(value, 0, Collections.newSetFromMap(new IdentityHashMap<>()), true);
    }

    private static Object convert(Object value, int depth, Set<Object> visiting, boolean allowCodec) {
        if (value == null || isLeaf(value)) {
            return value;
        }
        if (depth > MAX_DEPTH) {
            return "[MAX_DEPTH]";
        }
        // 사용자가 제외한 클래스는 트리로 펼치지 않는다 (excluded-classes)
        SuhLoggerProperties props = SuhLogger.getProperties();
        if (props != null && CommonUtil.isExcludedClass(value, props.getExcludedClasses())) {
            return CommonUtil.createExcludedClassInfo(value);
        }
        Object handled = TypeHandlers.apply(value);
        if (handled != null) {
            return handled;
        }
        if (isJpaEntity(value.getClass())) {
            // 엔티티 직렬화는 지연 로딩 쿼리를 일으킬 수 있어 2.x처럼 toString으로 둔다
            return value.toString();
        }
        if (!visiting.add(value)) {
            return "[CIRCULAR]";
        }
        try {
            if (value instanceof Map) {
                Map<Object, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                    out.put(String.valueOf(e.getKey()), convert(e.getValue(), depth + 1, visiting, allowCodec));
                }
                return out;
            }
            if (value instanceof Collection) {
                List<Object> out = new ArrayList<>();
                for (Object item : (Collection<?>) value) {
                    out.add(convert(item, depth + 1, visiting, allowCodec));
                }
                return out;
            }
            if (value.getClass().isArray()) {
                if (value.getClass().getComponentType().isPrimitive()) {
                    return "[" + value.getClass().getComponentType().getName() + " array]";
                }
                List<Object> out = new ArrayList<>();
                for (Object item : (Object[]) value) {
                    out.add(convert(item, depth + 1, visiting, allowCodec));
                }
                return out;
            }
            if (isPlatformType(value.getClass())) {
                return value.toString();
            }
            if (allowCodec) {
                Object viaCodec = viaCodec(value);
                if (viaCodec != null) {
                    return viaCodec;
                }
            }
            return reflect(value, depth, visiting);
        } finally {
            visiting.remove(value);
        }
    }

    /** 앱 ObjectMapper 설정(날짜 형식·@JsonIgnore·Kotlin 모듈)을 그대로 따르게 codec 우선 */
    private static Object viaCodec(Object value) {
        JsonCodec codec = JsonCodecs.get();
        if (codec == null) {
            return null;
        }
        try {
            return codec.parse(codec.write(value, false));
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    private static Object reflect(Object value, int depth, Set<Object> visiting) {
        Map<String, Object> out = new LinkedHashMap<>();
        Class<?> type = value.getClass();
        if (type.isRecord()) {
            for (RecordComponent c : type.getRecordComponents()) {
                try {
                    out.put(c.getName(), convert(c.getAccessor().invoke(value), depth + 1, visiting, false));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    out.put(c.getName(), "[UNREADABLE]");
                }
            }
            return out;
        }
        for (Class<?> c = type; c != null && c != Object.class && !isPlatformType(c); c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic() || out.containsKey(f.getName())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    out.put(f.getName(), convert(f.get(value), depth + 1, visiting, false));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    out.put(f.getName(), "[UNREADABLE]");
                }
            }
        }
        return out;
    }

    private static boolean isLeaf(Object v) {
        return v instanceof CharSequence || v instanceof Number || v instanceof Boolean || v instanceof Character
                || v instanceof Enum || v instanceof TemporalAccessor || v instanceof Date || v instanceof UUID
                || v instanceof Class;
    }

    /** JDK·프레임워크 내부 타입은 모듈 접근 제한·의미 없는 필드 때문에 toString으로 둔다 */
    private static boolean isPlatformType(Class<?> type) {
        String n = type.getName();
        return n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("jakarta.") || n.startsWith("sun.")
                || n.startsWith("jdk.") || n.startsWith("kotlin.") || n.startsWith("org.springframework.");
    }

    private static boolean isJpaEntity(Class<?> type) {
        for (java.lang.annotation.Annotation a : type.getAnnotations()) {
            String n = a.annotationType().getName();
            if (n.equals("jakarta.persistence.Entity") || n.equals("javax.persistence.Entity")) {
                return true;
            }
        }
        // Hibernate 프록시 (HibernateProxy) — 이름으로만 판별해 의존을 만들지 않는다
        return type.getName().contains("$HibernateProxy$");
    }
}
