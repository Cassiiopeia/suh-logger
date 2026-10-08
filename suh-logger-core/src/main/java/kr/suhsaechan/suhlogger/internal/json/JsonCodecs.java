package kr.suhsaechan.suhlogger.internal.json;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.spi.JsonCodecProvider;

/**
 * 현재 사용할 JsonCodec 보관소. 우선순위: 명시 등록(Spring이 앱 ObjectMapper로 등록) → ServiceLoader 공급자 → 없음.
 * 없을 때(null)도 로깅은 원문으로 계속된다.
 */
public final class JsonCodecs {

    private static volatile JsonCodec explicit;
    private static volatile JsonCodec discovered;
    private static volatile boolean discoveryDone;

    private JsonCodecs() {
    }

    public static void set(JsonCodec codec) {
        explicit = codec;
    }

    /** 테스트·컨텍스트 재시작용 */
    public static void reset() {
        explicit = null;
        discovered = null;
        discoveryDone = false;
    }

    public static JsonCodec get() {
        JsonCodec codec = explicit;
        if (codec != null) {
            return codec;
        }
        if (!discoveryDone) {
            discovered = discover();
            discoveryDone = true;
        }
        return discovered;
    }

    /** JSON이면 들여쓰기해서, 아니거나 codec이 없으면 원문 그대로 */
    public static String prettyOrRaw(String body) {
        JsonCodec codec = get();
        if (codec == null || body == null) {
            return body;
        }
        try {
            return codec.write(codec.parse(body), true);
        } catch (Exception | LinkageError e) {
            return body;
        }
    }

    private static JsonCodec discover() {
        List<JsonCodecProvider> providers = new ArrayList<>();
        Iterator<JsonCodecProvider> it = ServiceLoader.load(JsonCodecProvider.class,
                JsonCodecs.class.getClassLoader()).iterator();
        while (true) {
            try {
                if (!it.hasNext()) {
                    break;
                }
                providers.add(it.next());
            } catch (ServiceConfigurationError | LinkageError e) {
                // 깨진 공급자 하나 때문에 나머지를 못 쓰게 되면 안 된다
            }
        }
        providers.sort(Comparator.comparingInt(JsonCodecProvider::order));
        for (JsonCodecProvider provider : providers) {
            try {
                if (provider.isAvailable()) {
                    return provider.create();
                }
            } catch (RuntimeException | LinkageError e) {
                // 다음 공급자로
            }
        }
        return null;
    }
}
