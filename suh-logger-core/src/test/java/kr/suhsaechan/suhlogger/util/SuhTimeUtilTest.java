package kr.suhsaechan.suhlogger.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SuhTimeUtilTest {

    @Test
    void millisBoundaries() {
        assertEquals("0미리초", SuhTimeUtil.convertMillisToReadableTime(0));
        assertEquals("999미리초", SuhTimeUtil.convertMillisToReadableTime(999));
        assertEquals("1초", SuhTimeUtil.convertMillisToReadableTime(1_000));
        assertEquals("59초", SuhTimeUtil.convertMillisToReadableTime(59_999));
        assertEquals("1분 0초", SuhTimeUtil.convertMillisToReadableTime(60_000));
        assertEquals("2분 5초", SuhTimeUtil.convertMillisToReadableTime(125_000));
    }

    @Test
    void durationBoundaries() {
        assertEquals("850 미리초", SuhTimeUtil.convertDurationToReadableTime(Duration.ofMillis(850)));
        assertEquals("55 초", SuhTimeUtil.convertDurationToReadableTime(Duration.ofSeconds(55)));
        assertEquals("2 분 5 초", SuhTimeUtil.convertDurationToReadableTime(Duration.ofSeconds(125)));
        assertEquals("61 분 1 초", SuhTimeUtil.convertDurationToReadableTime(Duration.ofSeconds(3661)));
    }

    @Test
    void fixedDateTimeFormats() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 12, 9, 5, 3, 7_000_000);
        assertEquals("2026-10-12 09:05:03", SuhTimeUtil.BASIC_DATE_TIME_FORMATTER.format(t));
        assertEquals("20261012_090503", SuhTimeUtil.FILENAME_DATE_TIME_FORMATTER.format(t));
        assertEquals("2026-10-12 09:05:03.007", SuhTimeUtil.DATE_TIME_MILLIS_FORMATTER.format(t));
        assertEquals("2026년 10월 12일 09시 05분 03초", SuhTimeUtil.readableLocalDateTime(t));
    }

    @Test
    void nowHelpersMatchTheirFormats() {
        assertTrue(SuhTimeUtil.formatLocalDateTimeNow().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        assertTrue(SuhTimeUtil.formatLocalDateTimeMillisNow().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
        assertTrue(SuhTimeUtil.formatLocalDateTimeNowForFileName().matches("\\d{8}_\\d{6}"));
        assertTrue(SuhTimeUtil.readableCurrentLocalDateTime().contains("년"));
    }
}
