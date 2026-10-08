package kr.suhsaechan.suhlogger.internal.http;

import kr.suhsaechan.suhlogger.spi.HttpLogFormatter;

/** 사용자 HttpLogFormatter 보관소 (Spring은 빈으로, 순수 Java는 직접 set) */
public final class HttpLogFormatters {

    private static volatile HttpLogFormatter custom;

    private HttpLogFormatters() {
    }

    public static void set(HttpLogFormatter formatter) {
        custom = formatter;
    }

    public static HttpLogFormatter get() {
        return custom;
    }
}
