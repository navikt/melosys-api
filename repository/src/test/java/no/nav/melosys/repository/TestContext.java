package no.nav.melosys.repository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "no.nav.melosys.domain")
public class TestContext {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(TestContext.class, args);
    }
}
