package kr.suhsaechan.suhlogger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * SuhLogger 설정 프로퍼티
 * application.yml에서 suh-logger 관련 설정을 관리
 */
@ConfigurationProperties(prefix = "suh-logger")
public class SuhLoggerProperties {

    /**
     * 로깅에서 제외할 URL 패턴들
     */
    private List<String> excludePatterns = new ArrayList<>();

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

    // 기본 제외 패턴은 빈 배열로 시작 (사용자가 필요에 따라 설정)
    public SuhLoggerProperties() {
        // 기본값은 빈 배열
    }

    /**
     * 마스킹 설정 내부 클래스
     */
    public static class MaskingConfig {
        /**
         * 마스킹 활성화 여부 (기본값: false)
         * false로 설정하면 마스킹 없이 모든 데이터 로깅
         */
        private boolean enabled = false;

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

    public HeaderConfig getHeader() {
        return header;
    }

    public void setHeader(HeaderConfig header) {
        this.header = header;
    }
}
