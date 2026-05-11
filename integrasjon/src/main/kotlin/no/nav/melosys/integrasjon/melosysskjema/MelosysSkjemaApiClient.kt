package no.nav.melosys.integrasjon.melosysskjema

import mu.KotlinLogging
import no.nav.melosys.skjema.types.m2m.RegistrerSaksnummerRequest
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

private val log = KotlinLogging.logger { }

@Service
class MelosysSkjemaApiClient(private val melosysSkjemaApiWebClient: WebClient) {

    fun hentUtsendtArbeidstakerSkjema(skjemaId: UUID): UtsendtArbeidstakerSkjemaM2MDto {
        log.info("Henter utsendt arbeidstaker data for skjema med ID {}", skjemaId)

        return melosysSkjemaApiWebClient.get()
            .uri("/m2m/api/skjema/utsendt-arbeidstaker/{id}/data", skjemaId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(UtsendtArbeidstakerSkjemaM2MDto::class.java)
            .block() ?: error("Kunne ikke hente skjema for ID $skjemaId")
    }

    fun registrerSaksnummer(skjemaId: UUID, saksnummer: String) {
        log.info("Registrerer saksnummer {} for skjema {}", saksnummer, skjemaId)

        melosysSkjemaApiWebClient.post()
            .uri("/m2m/api/skjema/{id}/saksnummer", skjemaId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(RegistrerSaksnummerRequest(saksnummer))
            .retrieve()
            .toBodilessEntity()
            .block()
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

    fun hentVedleggInnhold(skjemaId: UUID, vedleggId: UUID): ByteArray {
        log.info("Henter vedlegg {} for skjema {}", vedleggId, skjemaId)

        return melosysSkjemaApiWebClient.get()
            .uri("/m2m/api/skjema/{skjemaId}/vedlegg/{vedleggId}/innhold", skjemaId, vedleggId)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block() ?: error("Kunne ikke hente vedlegg $vedleggId for skjema $skjemaId")
    }
}
