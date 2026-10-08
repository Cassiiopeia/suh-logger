package kr.suhsaechan.suhlogger.internal.http;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;

/**
 * 제외 경로 매칭. core는 Spring에 의존하지 않으므로 AntPathMatcher의 핵심 문법(*, **, ?)만 직접 구현한다.
 * 패턴 문자가 없는 값은 2.x처럼 contains로 해석한다 — "auth/login"이 "/auth/login-history"까지 제외하는 문제가 있어 deprecated.
 */
public final class PathPatterns {

    private static final Set<String> warned = ConcurrentHashMap.newKeySet();

    private PathPatterns() {
    }

    public static boolean anyMatch(List<String> patterns, String path) {
        if (patterns == null || path == null) {
            return false;
        }
        for (String p : patterns) {
            if (p != null && !p.isEmpty() && matches(p, path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(String pattern, String path) {
        if (!isPattern(pattern)) {
            if (warned.add(pattern)) {
                LoggerFactory.getLogger(PathPatterns.class).warn(
                        "[suh-logger] exclude pattern '{}' uses legacy contains matching (deprecated); "
                                + "use an Ant pattern such as '/{}/**'", pattern, pattern.replaceAll("^/+|/+$", ""));
            }
            return path.contains(pattern);
        }
        return ant(pattern, path);
    }

    static boolean isPattern(String p) {
        return p.indexOf('*') >= 0 || p.indexOf('?') >= 0 || p.indexOf('{') >= 0;
    }

    /** 세그먼트 단위 매칭: ** 는 0개 이상 세그먼트, * 와 ? 는 한 세그먼트 안에서만 */
    static boolean ant(String pattern, String path) {
        String[] p = trim(pattern).split("/");
        String[] s = trim(path).split("/");
        return match(p, 0, s, 0);
    }

    private static String trim(String v) {
        String t = v.startsWith("/") ? v.substring(1) : v;
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }

    private static boolean match(String[] p, int pi, String[] s, int si) {
        if (pi == p.length) {
            return si == s.length;
        }
        if (p[pi].equals("**")) {
            for (int k = si; k <= s.length; k++) {
                if (match(p, pi + 1, s, k)) {
                    return true;
                }
            }
            return false;
        }
        if (si == s.length) {
            return false;
        }
        return segment(p[pi], s[si]) && match(p, pi + 1, s, si + 1);
    }

    private static final java.util.Map<String, java.util.regex.Pattern> compiled = new ConcurrentHashMap<>();

    /**
     * 한 세그먼트 매칭. 와일드카드 외 문자는 전부 리터럴로 취급한다 — 사용자 패턴의 {, (, + 같은 문자가
     * 정규식으로 해석되면 PatternSyntaxException으로 요청 전체가 500이 된다.
     * {id} 같은 URI 변수는 Spring 관례대로 한 세그먼트 와일드카드로 본다.
     */
    private static boolean segment(String pattern, String value) {
        java.util.regex.Pattern regex = compiled.computeIfAbsent(pattern, PathPatterns::toRegex);
        return regex.matcher(value).matches();
    }

    private static java.util.regex.Pattern toRegex(String pattern) {
        StringBuilder sb = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*' || c == '?' || c == '{') {
                if (literal.length() > 0) {
                    sb.append(java.util.regex.Pattern.quote(literal.toString()));
                    literal.setLength(0);
                }
                if (c == '*') {
                    sb.append("[^/]*");
                } else if (c == '?') {
                    sb.append("[^/]");
                } else {
                    int close = pattern.indexOf('}', i);
                    if (close < 0) {
                        literal.append(c);
                        continue;
                    }
                    sb.append("[^/]+");
                    i = close;
                }
            } else {
                literal.append(c);
            }
        }
        if (literal.length() > 0) {
            sb.append(java.util.regex.Pattern.quote(literal.toString()));
        }
        return java.util.regex.Pattern.compile(sb.toString());
    }
}
