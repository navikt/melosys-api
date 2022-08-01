package no.nav.melosys.itest

import no.nav.melosys.melosysmock.config.GraphqlConfig
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import

@TestConfiguration
@Import(GraphqlConfig::class, KafkaTestConfig::class)
class SaksflytTestConfig
