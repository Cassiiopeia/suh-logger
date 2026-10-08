package kr.suhsaechan.suhlogger.config;

import java.util.ArrayList;
import java.util.List;
import kr.suhsaechan.suhlogger.internal.mask.SensitiveKeys;

/**
 * SuhLogger 설정 프로퍼티
 * application.yml에서 suh-logger 관련 설정을 관리
 *
 * Boot에서는 자동설정이 {@code @Bean @ConfigurationProperties("suh-logger")}로 바인딩한다.
 * core는 Boot에 의존하지 않으므로 이 클래스에는 바인딩 어노테이션을 두지 않는다.
 */
public class SuhLoggerProperties {

    /**
     * 로깅에서 제외할 URL 패턴들
     */
    private List<String> excludePatterns = new ArrayList<>(List.of("/actuator/**"));

    /**
     * 로깅 활성화 여부 (전체 제어)
     */
    private boolean enabled = true;

    /**
     * Response Body 로깅 최대 크기 (bytes)
     */
    private int maxResponseBodySize = 4096;

    /**
     * 마스킹 관련 설정
     */
    private MaskingConfig masking = new MaskingConfig();

    /**
     * 헤더 관련 설정
     */
    private HeaderConfig header = new HeaderConfig();

    /**
     * JSON 직렬화에서 제외할 클래스들
     */
    private List<String> excludedClasses = new ArrayList<>();

    /**
     * Response Body JSON pretty print 활성화 여부 (기본값: false)
     */
    private boolean prettyPrintJson = false;

    /**
     * 응답 본문 로깅 범위: none | error-only | all (기본값: all)
     */
    private ResponseBodyMode responseBody = ResponseBodyMode.ALL;

    /**
     * HTTP 요청 로그 형식: block | line (기본값: block)
     */
    private LogFormat format = LogFormat.BLOCK;

    /**
     * 이 시간(ms)을 넘긴 요청은 WARN으로 남긴다. 0이면 끔 (5xx는 항상 WARN)
     */
    private long slowThresholdMs = 0;

    /**
     * 요청 로깅 필터 순서 (기본값: 가장 마지막 — 보안 필터가 끝낸 요청도 최종 상태로 기록)
     */
    private int filterOrder = Integer.MAX_VALUE;

    /**
     * 요청 ID(MDC·응답 헤더) 설정
     */
    private RequestIdConfig requestId = new RequestIdConfig();

    // 기본 제외 패턴은 빈 배열로 시작 (사용자가 필요에 따라 설정)
    public SuhLoggerProperties() {
        // 기본값은 빈 배열
    }

    /**
     * 마스킹 설정 내부 클래스
     */
    public static class MaskingConfig {
        /**
         * 마스킹 활성화 여부 (기본값: true — 3.0부터. 2.x는 false여서 토큰이 평문으로 남았다)
         * false로 설정하면 마스킹 없이 모든 데이터 로깅
         */
        private boolean enabled = true;

        /**
         * 기본 민감 키 목록(password, token, secret, authorization 등)을 함께 쓸지 (기본값: true)
         * maskFields·maskHeaders에 적은 값은 기본 목록에 추가된다
         */
        private boolean useDefaults = true;

        /**
         * 추가 프리셋 (예: pii — email, phone 등 개인정보)
         */
        private List<String> presets = new ArrayList<>();

        /**
         * 마스킹할 헤더 키워드 목록
         * 헤더명에 이 키워드가 포함되면 마스킹 처리
         */
        private List<String> maskHeaders = new ArrayList<>();

        /**
         * 마스킹할 필드 키워드 목록
         * 필드명에 이 키워드가 포함되면 마스킹 처리
         */
        private List<String> maskFields = new ArrayList<>();

        /**
         * 마스킹 값 (기본값: ****)
         */
        private String maskValue = "****";

        /**
         * @deprecated 기존 호환성을 위해 유지, masking.enabled와 maskHeaders 사용 권장
         */
        @Deprecated
        private boolean header = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getMaskHeaders() {
            return maskHeaders;
        }

        public void setMaskHeaders(List<String> maskHeaders) {
            this.maskHeaders = maskHeaders;
        }

        public List<String> getMaskFields() {
            return maskFields;
        }

        public void setMaskFields(List<String> maskFields) {
            this.maskFields = maskFields;
        }

        public String getMaskValue() {
            return maskValue;
        }

        public void setMaskValue(String maskValue) {
            this.maskValue = maskValue;
        }

        public boolean isUseDefaults() {
            return useDefaults;
        }

        public void setUseDefaults(boolean useDefaults) {
            this.useDefaults = useDefaults;
        }

        public List<String> getPresets() {
            return presets;
        }

        public void setPresets(List<String> presets) {
            this.presets = presets;
        }

        /** 실제로 적용되는 필드 키: 기본 목록 + 프리셋 + 사용자 지정 */
        public List<String> effectiveMaskFields() {
            List<String> all = new ArrayList<>();
            if (useDefaults) {
                all.addAll(SensitiveKeys.DEFAULT_FIELDS);
            }
            if (presets != null) {
                presets.forEach(p -> all.addAll(SensitiveKeys.preset(p)));
            }
            if (maskFields != null) {
                all.addAll(maskFields);
            }
            return all;
        }

        /** 실제로 적용되는 헤더 키: 기본 목록 + 사용자 지정 */
        public List<String> effectiveMaskHeaders() {
            List<String> all = new ArrayList<>();
            if (useDefaults) {
                all.addAll(SensitiveKeys.DEFAULT_HEADERS);
            }
            if (maskHeaders != null) {
                all.addAll(maskHeaders);
            }
            return all;
        }

        @Deprecated
        public boolean isHeader() {
            return header;
        }

        @Deprecated
        public void setHeader(boolean header) {
            this.header = header;
        }
    }

    /**
     * 요청 ID 설정. 켜면 MDC에 넣어 앱 로그 패턴의 %X{requestId}로 이어지고 응답 헤더로 돌려준다.
     */
    public static class RequestIdConfig {

        /** 활성화 여부 (기본값: false) */
        private boolean enabled = false;

        /** 요청·응답 헤더 이름. 들어온 요청에 이 헤더가 있으면 그 값을 그대로 쓴다 */
        private String header = "X-Request-Id";

        /** MDC 키 */
        private String mdcKey = "requestId";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getMdcKey() {
            return mdcKey;
        }

        public void setMdcKey(String mdcKey) {
            this.mdcKey = mdcKey;
        }
    }

    /**
     * 헤더 설정 내부 클래스
     */
    public static class HeaderConfig {
        /**
         * 헤더 출력 활성화 여부 (기본값: false)
         * false로 설정하면 헤더 정보가 로그에 출력되지 않음
         */
        private boolean enabled = false;

        /**
         * 모든 헤더 출력 여부 (기본값: false)
         * true로 설정하면 모든 헤더를 출력
         * false이고 includeHeaders가 설정되어 있으면 해당 헤더만 출력
         */
        private boolean includeAll = false;

        /**
         * 출력할 헤더 목록
         * includeAll이 false일 때 이 목록에 있는 헤더만 출력
         */
        private List<String> includeHeaders = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIncludeAll() {
            return includeAll;
        }

        public void setIncludeAll(boolean includeAll) {
            this.includeAll = includeAll;
        }

        public List<String> getIncludeHeaders() {
            return includeHeaders;
        }

        public void setIncludeHeaders(List<String> includeHeaders) {
            this.includeHeaders = includeHeaders;
        }
    }

    // Getters and Setters
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxResponseBodySize() {
        return maxResponseBodySize;
    }

    public void setMaxResponseBodySize(int maxResponseBodySize) {
        this.maxResponseBodySize = maxResponseBodySize;
    }

    public MaskingConfig getMasking() {
        return masking;
    }

    public void setMasking(MaskingConfig masking) {
        this.masking = masking;
    }

    public List<String> getExcludedClasses() {
        return excludedClasses;
    }

    public void setExcludedClasses(List<String> excludedClasses) {
        this.excludedClasses = excludedClasses;
    }

    public boolean isPrettyPrintJson() {
        return prettyPrintJson;
    }

    public void setPrettyPrintJson(boolean prettyPrintJson) {
        this.prettyPrintJson = prettyPrintJson;
    }

    public ResponseBodyMode getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(ResponseBodyMode responseBody) {
        this.responseBody = responseBody;
    }

    public LogFormat getFormat() {
        return format;
    }

    public void setFormat(LogFormat format) {
        this.format = format;
    }

    public long getSlowThresholdMs() {
        return slowThresholdMs;
    }

    public void setSlowThresholdMs(long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    public int getFilterOrder() {
        return filterOrder;
    }

    public void setFilterOrder(int filterOrder) {
        this.filterOrder = filterOrder;
    }

    public RequestIdConfig getRequestId() {
        return requestId;
    }

    public void setRequestId(RequestIdConfig requestId) {
        this.requestId = requestId;
    }

    public HeaderConfig getHeader() {
        return header;
    }

    public void setHeader(HeaderConfig header) {
        this.header = header;
    }
}
