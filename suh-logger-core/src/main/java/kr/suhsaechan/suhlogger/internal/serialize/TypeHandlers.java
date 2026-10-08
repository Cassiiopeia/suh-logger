package kr.suhsaechan.suhlogger.internal.serialize;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import kr.suhsaechan.suhlogger.spi.TypeHandler;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TypeHandler 레지스트리. 런타임 등록분 + ServiceLoader 등록분 + 기본 핸들러를 order 순으로 검사한다.
 * 정적 상태인 이유: SuhLogger 정적 API가 Spring 컨텍스트 없이도 같은 규칙을 써야 하기 때문.
 */
public final class TypeHandlers {

    private static final Logger log = LoggerFactory.getLogger(TypeHandlers.class);
    private static final List<TypeHandler> registered = new CopyOnWriteArrayList<>();
    private static volatile List<TypeHandler> contextHandlers = List.of();
    private static volatile List<TypeHandler> cache;

    private TypeHandlers() {
    }

    public static void register(TypeHandler handler) {
        if (handler != null) {
            registered.add(handler);
            cache = null;
        }
    }

    /**
     * Spring 컨텍스트가 등록하는 핸들러 묶음을 통째로 바꾼다 — 테스트 컨텍스트·devtools 재시작마다 쌓여
     * 이전 클래스로더의 핸들러가 남지 않게. 직접 register()한 것은 건드리지 않는다.
     */
    public static void replaceContextHandlers(List<TypeHandler> handlers) {
        contextHandlers = handlers == null ? List.of() : List.copyOf(handlers);
        cache = null;
    }

    /** 테스트·컨텍스트 재시작용 */
    public static void reset() {
        registered.clear();
        contextHandlers = List.of();
        cache = null;
    }

    /** 처리할 핸들러가 없으면 null을 돌려 호출부가 기존 분기(Map·Collection 등)로 진행하게 한다 */
    public static Object apply(Object value) {
        for (TypeHandler handler : handlers()) {
            try {
                if (handler.supports(value)) {
                    return handler.toSafe(value);
                }
            } catch (RuntimeException e) {
                // 사용자 핸들러 실패가 비즈니스 호출을 깨면 안 된다
                log.debug("TypeHandler {} failed, falling back: {}", handler.getClass().getName(), e.toString());
                return null;
            }
        }
        return null;
    }

    private static List<TypeHandler> handlers() {
        List<TypeHandler> local = cache;
        if (local == null) {
            List<TypeHandler> all = new ArrayList<>(registered);
            all.addAll(contextHandlers);
            for (TypeHandler h : ServiceLoader.load(TypeHandler.class, TypeHandlers.class.getClassLoader())) {
                all.add(h);
            }
            all.addAll(defaults());
            all.sort(Comparator.comparingInt(TypeHandler::order));
            local = List.copyOf(all);
            cache = local;
        }
        return local;
    }

    /** 2.x CommonUtil에 하드코딩돼 있던 처리 순서를 그대로 옮긴 기본 핸들러 */
    static List<TypeHandler> defaults() {
        return List.of(
                handler(100, v -> v instanceof InputStream, v -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("_type", "InputStream");
                    m.put("_class", v.getClass().getName());
                    return m;
                }),
                handler(110, v -> v.getClass().getName().contains("MultipartFile"),
                        CommonUtil::extractMultipartFileInfo),
                handler(120, v -> v instanceof Vector, v -> CommonUtil.extractVectorInfo((Vector<?>) v)),
                handler(130, v -> v instanceof File, v -> CommonUtil.extractFileInfo((File) v)),
                handler(140, v -> v.getClass().getName().contains("org.locationtech.jts.geom")
                        || CommonUtil.isJTSGeometryType(v), CommonUtil::extractJTSGeometryInfo)
        );
    }

    private static TypeHandler handler(int order, Predicate<Object> supports, Function<Object, Object> toSafe) {
        return new TypeHandler() {
            @Override
            public boolean supports(Object value) {
                return supports.test(value);
            }

            @Override
            public Object toSafe(Object value) {
                return toSafe.apply(value);
            }

            @Override
            public int order() {
                return order;
            }
        };
    }
}
