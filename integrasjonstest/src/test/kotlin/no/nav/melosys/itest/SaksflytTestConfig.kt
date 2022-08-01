package no.nav.melosys.itest

import no.nav.melosys.melosysmock.config.GraphqlConfig
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@Import(GraphqlConfig::class, KafkaTestConfig::class)
class SaksflytTestConfig
