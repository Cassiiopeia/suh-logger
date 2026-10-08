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

    /** 자유 텍스트(예외 메시지)에 민감 키 단어가 들어 있는지 */
    public boolean containsSensitiveKey(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String k : keys) {
            if (lower.contains(k)) {
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

    private static final Pattern KEY = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*");

    /**
     * codec 없이 JSON 문자열을 훑어 민감 키의 값을 가린다. 값이 배열·객체면 짝이 맞는 괄호까지 통째로 가린다
     * (문자열 안의 괄호·이스케이프는 건너뛴다). JSON이 아니면 원문의 패턴 부분만 바뀐다.
     */
    String maskByPattern(String body) {
        StringBuilder out = new StringBuilder();
        Matcher m = KEY.matcher(body);
        int pos = 0;
        while (pos < body.length() && m.find(pos)) {
            int valueStart = m.end();
            out.append(body, pos, valueStart);
            int valueEnd = valueEnd(body, valueStart);
            if (isSensitive(m.group(1)) && valueEnd > valueStart) {
                out.append('"').append(maskValue).append('"');
            } else {
                out.append(body, valueStart, valueEnd);
            }
            pos = valueEnd;
            // 값이 객체·배열이면 안쪽 키도 검사해야 하므로 값 시작 직후부터 다시 찾는다
            if (!isSensitive(m.group(1)) && valueEnd > valueStart
                    && (body.charAt(valueStart) == '{' || body.charAt(valueStart) == '[')) {
                out.setLength(out.length() - (valueEnd - valueStart));
                out.append(body.charAt(valueStart));
                pos = valueStart + 1;
            }
        }
        out.append(body.substring(Math.min(pos, body.length())));
        return out.toString();
    }

    /** start 위치의 JSON 값 하나가 끝나는 인덱스 */
    private static int valueEnd(String s, int start) {
        if (start >= s.length()) {
            return start;
        }
        char c = s.charAt(start);
        if (c == '"') {
            return stringEnd(s, start);
        }
        if (c == '{' || c == '[') {
            int depth = 0;
            for (int i = start; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '"') {
                    i = stringEnd(s, i) - 1;
                } else if (ch == '{' || ch == '[') {
                    depth++;
                } else if (ch == '}' || ch == ']') {
                    if (--depth == 0) {
                        return i + 1;
                    }
                }
            }
            return s.length();
        }
        int i = start;
        while (i < s.length() && ",}] \t\r\n".indexOf(s.charAt(i)) < 0) {
            i++;
        }
        return i;
    }

    private static int stringEnd(String s, int quote) {
        for (int i = quote + 1; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                i++;
            } else if (ch == '"') {
                return i + 1;
            }
        }
        return s.length();
    }
}
