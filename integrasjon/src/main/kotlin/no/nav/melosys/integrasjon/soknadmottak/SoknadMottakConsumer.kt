package no.nav.melosys.integrasjon.soknadmottak

import mu.KotlinLogging
import no.nav.melosys.domain.msm.AltinnDokument
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Service
class SoknadMottakConsumer(private val soknadMottakWebClient: WebClient) {

    fun hentSøknad(søknadID: String): MedlemskapArbeidEOSM {
        log.info("Henter søknad med ID {}", søknadID)

        return soknadMottakWebClient.get()
            .uri("/soknader/{søknadID}", søknadID)
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .bodyToMono(MedlemskapArbeidEOSM::class.java)
            .block() ?: error("Kunne ikke hente body GET /soknader/$søknadID")
    }

    fun hentDokumenter(søknadID: String): Collection<AltinnDokument> {
        log.info("Henter dokumenter tilknyttet altinn-søknad {}", søknadID)

        return soknadMottakWebClient.get()
            .uri("/soknader/{søknadID}/dokumenter", søknadID)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Collection<AltinnDokument>>() {})
            .block() ?: error("Kunne ikke hente body for GET /soknader/$søknadID/dokumenter")
    }

}
