package no.nav.melosys.integrasjonstest.saksflyt;

import no.nav.melosys.Application;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


//@Testcontainers
@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "test")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = {Application.class}, properties = "spring.profiles.active:test", webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import({ComponentTestConfig.class})
@EnableMockOAuth2Server
public abstract class ComponentTestBase {

//    @Container
//    static OracleContainer DB = new OracleContainer();

//    @DynamicPropertySource
//    static void oracleProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", DB::getJdbcUrl);
//        registry.add("spring.datasource.password", DB::getPassword);
//        registry.add("spring.datasource.username", DB::getUsername);
//    }

}
