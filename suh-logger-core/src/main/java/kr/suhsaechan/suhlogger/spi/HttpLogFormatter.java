package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/**
 * HTTP 요청 로그 출력 형식 확장 지점. 등록하면 block/line 대신 이 결과를 한 줄 로그로 남긴다 (예: JSON 로그 수집기용).
 * 로그 레벨(5xx·느린 요청은 WARN)과 마스킹은 라이브러리가 처리하므로 문자열만 만들면 된다.
 */
@Incubating(since = "3.0.0")
@FunctionalInterface
public interface HttpLogFormatter {

    String format(HttpExchangeRecord record);
}
