package com.ai.manager.common.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DisplayTime 格式化断言：固定时刻在 Asia/Shanghai 时区下的输出。
 */
class DisplayTimeTest {

    private static final Instant FIXED =
            Instant.parse("2026-08-02T04:05:06Z"); // 上海时区 = 12:05:06

    @Test
    void formatMinute_shouldOutputShanghaiTimeWithSlash() {
        assertThat(DisplayTime.formatMinute(FIXED)).isEqualTo("2026/08/02 12:05");
    }

    @Test
    void formatSeconds_shouldOutputShanghaiTimeWithDash() {
        assertThat(DisplayTime.formatSeconds(FIXED)).isEqualTo("2026-08-02 12:05:06");
    }

    @Test
    void toZoned_shouldConvertToAsiaShanghai() {
        ZonedDateTime zoned = DisplayTime.toZoned(FIXED);

        assertThat(zoned.getZone()).isEqualTo(DisplayTime.ZONE);
        assertThat(zoned.toOffsetDateTime().getOffset()).isEqualTo(ZoneOffset.ofHours(8));
        assertThat(zoned.getHour()).isEqualTo(12);
    }
}
