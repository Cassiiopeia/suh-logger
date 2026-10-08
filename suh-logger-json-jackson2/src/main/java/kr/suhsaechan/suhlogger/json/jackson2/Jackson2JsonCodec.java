package kr.suhsaechan.suhlogger.json.jackson2;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.suhsaechan.suhlogger.spi.JsonCodec;

/** Jackson 2 기반 JsonCodec. Boot 3에서는 앱의 ObjectMapper 빈으로 만들어 날짜·Kotlin 모듈 설정을 그대로 쓴다 */
public class Jackson2JsonCodec implements JsonCodec {

    private final ObjectMapper mapper;

    public Jackson2JsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object parse(String json) throws Exception {
        return mapper.readValue(json, Object.class);
    }

    @Override
    public String write(Object value, boolean pretty) throws Exception {
        return pretty ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
                : mapper.writeValueAsString(value);
    }
}
