package com.wanted.boothub.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "오늘" 기준 시각. 서버 시간대와 상관없이 한국 시간으로 계산하고, 테스트에서 바꿔 끼울 수 있게 빈으로 둔다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
