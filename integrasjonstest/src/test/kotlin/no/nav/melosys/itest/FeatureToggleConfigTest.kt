package no.nav.melosys.itest

import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val log = KotlinLogging.logger { }

@Configuration
@Profile("test")
class FeatureToggleConfigTest {
    @Bean
    fun unleash(): Unleash = FakeUnleash().apply { enableAll() }.also {
        log.info { "FeatureToggleConfigTest er aktivert med ${it.javaClass.simpleName}" }
    }
}
