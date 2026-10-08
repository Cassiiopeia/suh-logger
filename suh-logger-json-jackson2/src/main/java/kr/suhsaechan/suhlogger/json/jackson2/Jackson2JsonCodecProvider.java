package kr.suhsaechan.suhlogger.json.jackson2;

import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.spi.JsonCodecProvider;

/** Jackson 2가 classpath에 있을 때만 기본 ObjectMapper로 codec을 만든다 (필드·시그니처에 Jackson 타입 없음) */
public class Jackson2JsonCodecProvider implements JsonCodecProvider {

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper", false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public JsonCodec create() {
        return new Jackson2JsonCodec(new com.fasterxml.jackson.databind.ObjectMapper());
    }

    /** Jackson 3가 함께 있으면 Boot 4 기본인 3을 먼저 쓴다 */
    @Override
    public int order() {
        return 20;
    }
}
