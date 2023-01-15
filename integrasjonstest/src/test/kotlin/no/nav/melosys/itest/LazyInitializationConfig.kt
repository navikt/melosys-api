package no.nav.melosys.itest

import no.nav.melosys.Application
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [Application::class])
@TestPropertySource(properties = ["spring.main.lazy-initialization=true"])
interface LazyInitializationConfig {
}
