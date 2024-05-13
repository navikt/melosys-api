package no.nav.melosys.itest

import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import no.nav.melosys.melosysmock.config.GraphqlConfig
import no.nav.melosys.melosysmock.config.SoapConfig
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@TestConfiguration
@Import(GraphqlConfig::class, SoapConfig::class, KafkaTestConfig::class)
class ComponentTestConfig {
    @Bean
    @Primary
    fun fakeUnleash(): Unleash {
        return FakeUnleash()
    }
}
