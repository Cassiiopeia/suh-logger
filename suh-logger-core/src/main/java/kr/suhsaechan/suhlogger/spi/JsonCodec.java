package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/**
 * JSON 파싱·직렬화 확장 지점. core가 Jackson 2(Boot 3)·Jackson 3(Boot 4) 어느 쪽에도 묶이지 않게 분리했다.
 * Boot에서는 앱의 ObjectMapper/JsonMapper 빈으로 만든 구현이 자동으로 쓰인다.
 */
@Incubating(since = "3.0.0")
public interface JsonCodec {

    /** JSON 문자열을 Map·List·값 트리로 */
    Object parse(String json) throws Exception;

    /** 값을 JSON 문자열로 (pretty면 들여쓰기) */
    String write(Object value, boolean pretty) throws Exception;
}
