package me.suhsaechan.suhlogger.config;

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

    // 기본 제외 패턴은 빈 배열로 시작 (사용자가 필요에 따라 설정)
    public SuhLoggerProperties() {
        // 기본값은 빈 배열
    }

    /**
     * 마스킹 설정 내부 클래스
     */
    public static class MaskingConfig {
        /**
         * 헤더 마스킹 활성화 여부 (기본값: true)
         */
        private boolean header = true;

        public boolean isHeader() {
            return header;
        }

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

    public HeaderConfig getHeader() {
        return header;
    }

    public void setHeader(HeaderConfig header) {
        this.header = header;
    }
}
