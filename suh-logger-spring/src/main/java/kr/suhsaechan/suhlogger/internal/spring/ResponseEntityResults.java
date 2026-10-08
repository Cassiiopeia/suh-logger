package kr.suhsaechan.suhlogger.internal.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.http.ResponseEntity;

/**
 * ResponseEntity 전용 처리. spring-web이 classpath에 있을 때만 이 클래스가 로드되도록
 * 호출부(aspect)가 클래스 이름 비교를 먼저 거친다.
 */
public final class ResponseEntityResults {

    private ResponseEntityResults() {
    }

    public static Map<String, Object> toSafeMap(Object result,
                                                Function<Map<String, String>, Map<String, String>> headerMasker,
                                                Predicate<Object> isComplex) {
        ResponseEntity<?> entity = (ResponseEntity<?>) result;
        Map<String, Object> safe = new HashMap<>();
        safe.put("statusCode", entity.getStatusCode().toString());
        safe.put("statusCodeValue", entity.getStatusCode().value());
        safe.put("headers", headerMasker.apply(entity.getHeaders().toSingleValueMap()));
        Object body = entity.getBody();
        if (body != null) {
            if (isComplex.test(body)) {
                // 복잡한 객체는 필터가 응답 본문으로 따로 남긴다
                safe.put("bodyType", body.getClass().getSimpleName());
                safe.put("bodyInfo", "Complex object - logged separately by filter");
            } else {
                safe.put("body", body);
            }
        }
        return safe;
    }
}
