package no.nav.melosys.integrasjon.faktureringskomponenten

import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class NyFakturaserieResponseDto(
    val fakturaserieReferanse: String,
)

open class FaktureringskomponentenConsumer(private val webClient: WebClient) : JsonRestIntegrasjon {

    fun lagFakturaSerie(fakturaserieDto: FakturaserieDto) =
        webClient.post()
            .uri("/fakturaserier")
            .bodyValue(fakturaserieDto)
            .retrieve()
            .bodyToMono<NyFakturaserieResponseDto>()
            .block()!!
}
