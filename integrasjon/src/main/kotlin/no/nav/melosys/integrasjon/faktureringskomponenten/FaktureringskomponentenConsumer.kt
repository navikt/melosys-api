package no.nav.melosys.integrasjon.faktureringskomponenten

import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaMottakerDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class NyFakturaserieResponseDto(
    val fakturaserieReferanse: String,
)

open class FaktureringskomponentenConsumer(private val webClient: WebClient) : JsonRestIntegrasjon {

    fun lagFakturaserie(fakturaserieDto: FakturaserieDto, saksbehandlerIdent: String) =
        webClient.post()
            .uri("/fakturaserier")
            .header("Nav-User-Id", saksbehandlerIdent)
            .bodyValue(fakturaserieDto)
            .retrieve()
            .bodyToMono<NyFakturaserieResponseDto>()
            .block()!!

    fun kansellerFakturaserie(referanse: String, saksbehandlerIdent: String) =
        webClient.delete()
            .uri("/fakturaserier/{referanse}", referanse)
            .header("Nav-User-Id", saksbehandlerIdent)
            .retrieve()
            .bodyToMono<NyFakturaserieResponseDto>()
            .block()!!

    fun oppdaterFakturaMottaker(referanse: String, fakturaMottakerDto: FakturaMottakerDto, saksbehandlerIdent: String) =
        webClient.put()
            .uri("/fakturaserier/{referanse}/mottaker", referanse)
            .header("Nav-User-Id", saksbehandlerIdent)
            .bodyValue(fakturaMottakerDto)
            .retrieve()
            .bodyToMono<Void>()
            .block()

    fun getFakturaSerie(referanse: String) =
        webClient.get()
            .uri("/fakturaserier/{referanse}", referanse)
            .retrieve()
            .bodyToMono<FakturaserieDto>()
            .block()
}
