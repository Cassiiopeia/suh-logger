package kr.suhsaechan.suhlogger.json.jackson3;

import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.spi.JsonCodecProvider;

/** Jackson 3가 classpath에 있을 때만 기본 JsonMapper로 codec을 만든다 */
public class Jackson3JsonCodecProvider implements JsonCodecProvider {

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("tools.jackson.databind.json.JsonMapper", false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public JsonCodec create() {
        return new Jackson3JsonCodec(tools.jackson.databind.json.JsonMapper.builder().build());
    }

    @Override
    public int order() {
        return 10;
    }
}
