package no.nav.melosys;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30m")
public class SchedulingConfig {

    @Bean
    @DependsOnDatabaseInitialization
    public JdbcTemplateLockProvider jdbcTemplateLockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
