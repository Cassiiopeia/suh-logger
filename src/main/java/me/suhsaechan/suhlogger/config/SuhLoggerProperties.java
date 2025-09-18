package me.suhsaechan.suhlogger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SuhLogger 설정 프로퍼티
 * application.yml에서 suh-logger 관련 설정을 관리
 */
@Component
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

    // 기본 제외 패턴은 빈 배열로 시작 (사용자가 필요에 따라 설정)
    public SuhLoggerProperties() {
        // 기본값은 빈 배열
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
}
