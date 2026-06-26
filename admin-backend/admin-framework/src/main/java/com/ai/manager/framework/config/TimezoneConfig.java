package com.ai.manager.framework.config;

import com.ai.manager.common.time.DisplayTime;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

    @PostConstruct
    void configureDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(DisplayTime.ZONE));
    }
}
