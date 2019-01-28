package no.nav.melosys;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    Flyway flyway(DataSource datasource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(datasource);
        flyway.setLocations("classpath:db/migration/melosysDB");
        flyway.setBaselineOnMigrate(true);

        return flyway;
    }

    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }
}
