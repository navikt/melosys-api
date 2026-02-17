package no.nav.melosys.integrasjon.melosysskjema

import mu.KotlinLogging
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

private val log = KotlinLogging.logger { }

@Service
class MelosysSkjemaApiClient(private val melosysSkjemaApiWebClient: WebClient) {

    fun hentUtsendtArbeidstakerSkjema(skjemaId: UUID): UtsendtArbeidstakerM2MSkjemaData {
        log.info("Henter utsendt arbeidstaker data for skjema med ID {}", skjemaId)

        return melosysSkjemaApiWebClient.get()
            .uri("/m2m/api/skjema/utsendt-arbeidstaker/{id}/data", skjemaId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(UtsendtArbeidstakerM2MSkjemaData::class.java)
            .block() ?: error("Kunne ikke hente skjema for ID $skjemaId")
    }

    fun hentPdf(skjemaId: UUID): ByteArray {
        log.info("Henter PDF for skjema med ID {}", skjemaId)

        return melosysSkjemaApiWebClient.get()
            .uri("/m2m/api/skjema/{id}/pdf", skjemaId)
            .accept(MediaType.APPLICATION_PDF)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block() ?: error("Kunne ikke hente PDF for skjema $skjemaId")
    }
}
