package com.logistics.config;

import com.logistics.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

/** Enables @Scheduled jobs and JPA auditing (created_at/updated_at/created_by/updated_by). */
@Configuration
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class SchedulerConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.ofNullable(SecurityUtils.currentUserId());
    }
}
