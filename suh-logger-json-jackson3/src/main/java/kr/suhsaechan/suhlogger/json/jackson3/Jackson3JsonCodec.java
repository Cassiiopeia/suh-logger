package kr.suhsaechan.suhlogger.json.jackson3;

import kr.suhsaechan.suhlogger.spi.JsonCodec;
import tools.jackson.databind.ObjectMapper;

/** Jackson 3 기반 JsonCodec. Boot 4에서는 앱의 JsonMapper 빈으로 만든다 */
public class Jackson3JsonCodec implements JsonCodec {

    private final ObjectMapper mapper;

    public Jackson3JsonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object parse(String json) {
        return mapper.readValue(json, Object.class);
    }

    @Override
    public String write(Object value, boolean pretty) {
        return pretty ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
                : mapper.writeValueAsString(value);
    }
}
