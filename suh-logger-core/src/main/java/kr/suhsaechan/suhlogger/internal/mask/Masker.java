package kr.suhsaechan.suhlogger.internal.mask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.spi.JsonCodec;

/** 트리·JSON 문자열에서 민감 키의 값을 가린다. 키가 걸리면 하위 객체 전체를 가린다 */
public final class Masker {

    private final List<String> keys;
    private final String maskValue;

    public Masker(List<String> keys, String maskValue) {
        List<String> lower = new ArrayList<>();
        for (String k : keys) {
            if (k != null && !k.isBlank()) {
                lower.add(k.toLowerCase(Locale.ROOT));
            }
        }
        this.keys = lower;
        this.maskValue = maskValue != null ? maskValue : "****";
    }

    public boolean isSensitive(String name) {
        if (name == null || keys.isEmpty()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        for (String k : keys) {
            if (n.contains(k)) {
                return true;
            }
        }
        return false;
    }

    /** ObjectTrees 결과(Map·List·값)를 복사하며 마스킹 */
    public Object mask(Object tree) {
        if (tree instanceof Map) {
            Map<Object, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) tree).entrySet()) {
                String key = String.valueOf(e.getKey());
                out.put(e.getKey(), isSensitive(key) ? maskValue : mask(e.getValue()));
            }
            return out;
        }
        if (tree instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) tree) {
                out.add(mask(item));
            }
            return out;
        }
        return tree;
    }

    /** JSON 응답 본문 마스킹. codec이 있으면 트리로, 없거나 JSON이 아니면 "key": value 패턴만 정규식으로 가린다 */
    public String maskJson(String body, boolean pretty) {
        if (body == null || keys.isEmpty()) {
            return body;
        }
        JsonCodec codec = JsonCodecs.get();
        if (codec != null) {
            try {
                return codec.write(mask(codec.parse(body)), pretty);
            } catch (Exception | LinkageError e) {
                // JSON이 아니면 정규식으로
            }
        }
        return maskByPattern(body);
    }

    private static final Pattern PAIR = Pattern.compile(
            "(\"([^\"\\\\]*)\"\\s*:\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"|-?\\d[\\d.eE+-]*|true|false|null)");

    String maskByPattern(String body) {
        Matcher m = PAIR.matcher(body);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String replacement = isSensitive(m.group(2)) ? m.group(1) + "\"" + maskValue + "\"" : m.group(0);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
