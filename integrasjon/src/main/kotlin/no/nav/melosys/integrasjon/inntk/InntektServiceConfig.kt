package no.nav.melosys.integrasjon.inntk

import no.finn.unleash.Unleash
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.integrasjon.inntk.inntekt.InntektConsumer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InntektServiceConfig(
    private var unleash: Unleash,
    private val inntektConsumer: InntektConsumer,
    private val dokumentFactory: DokumentFactory,
) {

    @Bean
    fun InntektFasade(): InntektFasade {
        if (unleash.isEnabled("melosys.rest.inntekt")) {
            return InntektRestService(inntektConsumer, dokumentFactory)
        }
        return InntektService(inntektConsumer, dokumentFactory)
    }
}
