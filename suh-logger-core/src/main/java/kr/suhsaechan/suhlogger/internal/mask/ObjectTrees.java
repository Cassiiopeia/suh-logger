package kr.suhsaechan.suhlogger.internal.mask;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.internal.serialize.TypeHandlers;
import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import kr.suhsaechan.suhlogger.util.SuhLogger;

/**
 * 객체를 Map·List·값 트리로 바꾼다 — 필드 단위 마스킹과 읽을 수 있는 출력을 위해.
 * 2.x는 DTO를 toString()으로 찍어 record·Lombok @Data의 토큰이 그대로 노출됐다.
 * 원칙: 마스킹을 거칠 수 없는 문자열(toString)은 값 타입에만 허용하고, 그 밖의 타입은 펼치거나 타입 이름만 남긴다.
 */
public final class ObjectTrees {

    private static final int MAX_DEPTH = 8;
    /** 컬렉션 하나에서 펼칠 최대 원소 수 — 10만 건 리스트를 반환해도 로깅 비용이 묶이게 */
    static final int MAX_ELEMENTS = 100;
    /** 한 번 변환에서 만들 최대 노드 수 */
    static final int MAX_NODES = 2_000;

    /** 클래스별 "그래프 안에 JPA 엔티티가 있는가" 캐시 — codec(Jackson)이 lazy 연관 getter를 부르지 않게 */
    private static final Map<Class<?>, Boolean> reachesEntity = new ConcurrentHashMap<>();

    private ObjectTrees() {
    }

    public static Object toTree(Object value) {
        return new Walker().convert(value, 0, true);
    }

    private static final class Walker {

        private final Set<Object> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
        private int nodes;

        Object convert(Object value, int depth, boolean allowCodec) {
            if (value == null || isLeaf(value)) {
                return value;
            }
            if (depth > MAX_DEPTH) {
                return "[MAX_DEPTH]";
            }
            if (++nodes > MAX_NODES) {
                return "[TRUNCATED]";
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
            if (isHibernateProxy(value.getClass())) {
                // 초기화 안 된 프록시를 건드리면 쿼리가 나간다
                return "[proxy " + entityName(value.getClass()) + "]";
            }
            if (!visiting.add(value)) {
                return "[CIRCULAR]";
            }
            try {
                return expand(value, depth, allowCodec);
            } finally {
                visiting.remove(value);
            }
        }

        private Object expand(Object value, int depth, boolean allowCodec) {
            if (value instanceof Optional) {
                return convert(((Optional<?>) value).orElse(null), depth + 1, allowCodec);
            }
            if (value instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) value;
                Map<String, Object> out = new LinkedHashMap<>();
                out.put(String.valueOf(e.getKey()), convert(e.getValue(), depth + 1, allowCodec));
                return out;
            }
            if (value instanceof Map) {
                Map<Object, Object> out = new LinkedHashMap<>();
                int i = 0;
                for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                    if (i++ == MAX_ELEMENTS) {
                        out.put("...", "(" + (((Map<?, ?>) value).size() - MAX_ELEMENTS) + " more)");
                        break;
                    }
                    out.put(String.valueOf(e.getKey()), convert(e.getValue(), depth + 1, allowCodec));
                }
                return out;
            }
            if (value instanceof Collection) {
                if (isLazyCollection(value.getClass())) {
                    // size()·iterator()가 지연 로딩을 일으키므로 열지 않는다
                    return "[lazy collection]";
                }
                return list(((Collection<?>) value), ((Collection<?>) value).size(), depth, allowCodec);
            }
            if (value.getClass().isArray()) {
                if (value.getClass().getComponentType().isPrimitive()) {
                    return "[" + value.getClass().getComponentType().getName() + " array]";
                }
                Object[] array = (Object[]) value;
                return list(java.util.Arrays.asList(array), array.length, depth, allowCodec);
            }
            Object wrapper = unwrapKnownContainer(value, depth, allowCodec);
            if (wrapper != null) {
                return wrapper;
            }
            Class<?> type = value.getClass();
            if (isJpaEntity(type)) {
                // 엔티티는 필드를 직접 읽되 연관(엔티티·컬렉션)은 열지 않는다 — toString()은 마스킹을 우회하고 지연 로딩을 부른다
                return reflect(value, depth, true);
            }
            if (isPlatformType(type)) {
                return isValueType(type) ? value.toString() : "[" + type.getName() + "]";
            }
            if (allowCodec && !reachesEntity(type)) {
                Object viaCodec = viaCodec(value);
                if (viaCodec != null) {
                    return viaCodec;
                }
            }
            return reflect(value, depth, false);
        }

        private List<Object> list(Iterable<?> items, int size, int depth, boolean allowCodec) {
            List<Object> out = new ArrayList<>();
            int i = 0;
            for (Object item : items) {
                if (i++ == MAX_ELEMENTS) {
                    out.add("(" + (size - MAX_ELEMENTS) + " more)");
                    break;
                }
                out.add(convert(item, depth + 1, allowCodec));
            }
            return out;
        }

        /**
         * 마스킹을 우회하던 "내용을 감싼" 타입들을 이름 기반 리플렉션으로 펼친다 (프레임워크 의존 없이).
         * Boot 4의 HttpHeaders는 더 이상 Map이 아니라 toString()에 Authorization 원문이 들어 있었다.
         */
        private Object unwrapKnownContainer(Object value, int depth, boolean allowCodec) {
            String name = value.getClass().getName();
            try {
                if (instanceOf(value.getClass(), "java.util.concurrent.atomic.AtomicReference")) {
                    return convert(value.getClass().getMethod("get").invoke(value), depth + 1, allowCodec);
                }
                if (instanceOf(value.getClass(), "org.springframework.http.HttpHeaders")) {
                    Object single = value.getClass().getMethod("toSingleValueMap").invoke(value);
                    return convert(single, depth + 1, allowCodec);
                }
                if (instanceOf(value.getClass(), "org.springframework.http.HttpEntity")) {
                    Map<String, Object> out = new LinkedHashMap<>();
                    if (instanceOf(value.getClass(), "org.springframework.http.ResponseEntity")) {
                        out.put("status", String.valueOf(value.getClass().getMethod("getStatusCode").invoke(value)));
                    }
                    out.put("headers", convert(value.getClass().getMethod("getHeaders").invoke(value), depth + 1, allowCodec));
                    out.put("body", convert(value.getClass().getMethod("getBody").invoke(value), depth + 1, allowCodec));
                    return out;
                }
                if (name.equals("kotlin.Pair") || name.equals("kotlin.Triple")) {
                    List<Object> out = new ArrayList<>();
                    out.add(convert(value.getClass().getMethod("getFirst").invoke(value), depth + 1, allowCodec));
                    out.add(convert(value.getClass().getMethod("getSecond").invoke(value), depth + 1, allowCodec));
                    if (name.equals("kotlin.Triple")) {
                        out.add(convert(value.getClass().getMethod("getThird").invoke(value), depth + 1, allowCodec));
                    }
                    return out;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                return "[" + name + "]";
            }
            return null;
        }

        private Object reflect(Object value, int depth, boolean entity) {
            Map<String, Object> out = new LinkedHashMap<>();
            Class<?> type = value.getClass();
            if (type.isRecord()) {
                for (RecordComponent c : type.getRecordComponents()) {
                    try {
                        out.put(c.getName(), convert(c.getAccessor().invoke(value), depth + 1, false));
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        out.put(c.getName(), "[UNREADABLE]");
                    }
                }
                return out;
            }
            for (Class<?> c = type; c != null && c != Object.class && !isPlatformType(c); c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()
                            || out.containsKey(f.getName())) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                        Object fieldValue = f.get(value);
                        if (entity && fieldValue != null && isAssociation(fieldValue)) {
                            out.put(f.getName(), "[" + associationLabel(fieldValue) + "]");
                        } else {
                            out.put(f.getName(), convert(fieldValue, depth + 1, false));
                        }
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        out.put(f.getName(), "[UNREADABLE]");
                    }
                }
            }
            return out;
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

    private static boolean isLeaf(Object v) {
        return v instanceof CharSequence || v instanceof Number || v instanceof Boolean || v instanceof Character
                || v instanceof Enum || v instanceof TemporalAccessor || v instanceof Date || v instanceof UUID
                || v instanceof Class;
    }

    /** JDK·프레임워크 내부 타입은 모듈 접근 제한·의미 없는 필드 때문에 리플렉션하지 않는다 */
    private static boolean isPlatformType(Class<?> type) {
        String n = type.getName();
        if (n.startsWith("org.springframework.data.domain.")) {
            // Page·Slice는 사용자 데이터 컨테이너라 일반 객체처럼 펼친다
            return false;
        }
        return n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("jakarta.") || n.startsWith("sun.")
                || n.startsWith("jdk.") || n.startsWith("kotlin.") || n.startsWith("org.springframework.");
    }

    /** toString()을 그대로 남겨도 되는 값 타입 (사용자 객체를 감싸지 않는 것만) */
    private static boolean isValueType(Class<?> type) {
        if (TemporalAmount.class.isAssignableFrom(type) || Throwable.class.isAssignableFrom(type)) {
            return true;
        }
        String n = type.getName();
        return n.startsWith("java.net.") || n.startsWith("java.math.") || n.startsWith("java.nio.file.")
                || n.startsWith("java.nio.charset.") || n.startsWith("java.time.")
                || n.equals("java.util.Locale") || n.equals("java.util.Currency") || n.equals("java.util.TimeZone")
                || n.equals("java.util.regex.Pattern") || n.equals("kotlin.Unit");
    }

    private static boolean instanceOf(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    static boolean isJpaEntity(Class<?> type) {
        for (java.lang.annotation.Annotation a : type.getAnnotations()) {
            String n = a.annotationType().getName();
            if (n.equals("jakarta.persistence.Entity") || n.equals("javax.persistence.Entity")) {
                return true;
            }
        }
        return false;
    }

    /** Hibernate 프록시 — 의존을 만들지 않으려고 이름·인터페이스로만 판별한다 */
    private static boolean isHibernateProxy(Class<?> type) {
        if (type.getName().contains("$HibernateProxy")) {
            return true;
        }
        for (Class<?> i : type.getInterfaces()) {
            if (i.getName().equals("org.hibernate.proxy.HibernateProxy")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLazyCollection(Class<?> type) {
        return type.getName().startsWith("org.hibernate.collection.");
    }

    private static boolean isAssociation(Object fieldValue) {
        Class<?> t = fieldValue.getClass();
        return isJpaEntity(t) || isHibernateProxy(t) || fieldValue instanceof Collection || fieldValue instanceof Map;
    }

    private static String associationLabel(Object fieldValue) {
        if (fieldValue instanceof Collection || fieldValue instanceof Map) {
            return "association";
        }
        return entityName(fieldValue.getClass());
    }

    private static String entityName(Class<?> type) {
        String n = type.getSimpleName();
        int proxy = n.indexOf('$');
        return proxy > 0 ? n.substring(0, proxy) : n;
    }

    /** 객체 그래프(필드 타입·제네릭 인자, 깊이 3)에 엔티티가 있는지 — 있으면 codec 대신 연관을 열지 않는 리플렉션을 쓴다 */
    private static boolean reachesEntity(Class<?> type) {
        return reachesEntity.computeIfAbsent(type, t -> scan(t, 0, Collections.newSetFromMap(new IdentityHashMap<>())));
    }

    private static boolean scan(Class<?> type, int depth, Set<Class<?>> seen) {
        if (type == null || depth > 3 || type.isPrimitive() || !seen.add(type)) {
            return false;
        }
        if (isJpaEntity(type)) {
            return true;
        }
        if (isPlatformType(type)) {
            return false;
        }
        for (Class<?> c = type; c != null && c != Object.class && !isPlatformType(c); c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (scan(f.getType(), depth + 1, seen) || scanGeneric(f.getGenericType(), depth + 1, seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean scanGeneric(Type t, int depth, Set<Class<?>> seen) {
        if (t instanceof ParameterizedType) {
            for (Type arg : ((ParameterizedType) t).getActualTypeArguments()) {
                if (arg instanceof Class && scan((Class<?>) arg, depth, seen)) {
                    return true;
                }
            }
        }
        return false;
    }
}
