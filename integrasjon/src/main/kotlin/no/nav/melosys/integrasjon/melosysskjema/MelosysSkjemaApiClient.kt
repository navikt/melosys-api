package no.nav.melosys.integrasjon.melosysskjema

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Service
class MelosysSkjemaApiClient(private val melosysSkjemaWebClient: WebClient) {
    // TODO: Legg til metoder for å kalle melosys-skjema API
}
