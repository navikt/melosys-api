package no.nav.melosys.integrasjon.soknadmottak

import no.nav.melosys.domain.msm.AltinnDokument
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

open class SoknadMottakConsumerImpl(private val webClient: WebClient) : SoknadMottakConsumer {

    companion object {
        private val log = LoggerFactory.getLogger(SoknadMottakConsumerImpl::class.java)
    }

    override fun hentSøknad(søknadID: String): MedlemskapArbeidEOSM? {
        log.info("Henter søknad med ID {}", søknadID)

        return webClient.get()
            .uri("/soknader/{søknadID}", søknadID)
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .toEntity(MedlemskapArbeidEOSM::class.java)
            .block()?.body
    }

    override fun hentDokumenter(søknadID: String): Collection<AltinnDokument>? {
        log.info("Henter dokumenter tilknyttet altinn-søknad {}", søknadID)

        return webClient.get()
            .uri("/soknader/{søknadID}/dokumenter", søknadID)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(object : ParameterizedTypeReference<Collection<AltinnDokument>>() {})
            .block()?.body
    }

}
