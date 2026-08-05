package com.ezdo.config;

import org.springframework.context.annotation.Configuration;

/**
 * Quartz Scheduler Configuration
 *
 * Spring Boot auto-configures Quartz with JDBC persistence based on application.yml.
 *
 * The application uses:
 * - JobStoreTX: Quartz manages its own transactions (recommended for async jobs)
 * - MySQL database with QRTZ_* tables for job persistence
 * - Jobs survive application restarts
 *
 * Alternative: Use LocalDataSourceJobStore if you need Spring transaction participation
 * (requires changing jobStore.class in application.yml)
 */
@Configuration
public class QuartzConfig {
    // Spring Boot auto-configuration handles everything based on application.yml
    // No manual beans needed unless you want to customize further
}
