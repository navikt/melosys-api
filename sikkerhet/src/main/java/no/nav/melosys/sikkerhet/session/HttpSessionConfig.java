package no.nav.melosys.sikkerhet.session;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJdbcHttpSession
public class HttpSessionConfig {

    @Bean
    JdbcOperationsSessionRepository sessionRepository(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        return new JdbcOperationsSessionRepository(jdbcTemplate, transactionManager);
    }
}
