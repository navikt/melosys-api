package no.nav.melosys.integrasjon.faktureringskomponenten

import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

data class FaktureringskomponentResponse(
    val type: String,
    val title: String?,
    val status: Int,
    val instance: String?,
    val referanseId: String?,
    val detail: String?
)

open class FaktureringskomponentenConsumer(private val webClient: WebClient) : JsonRestIntegrasjon {

    fun lagFakturaSerie(fakturaserieDto: FakturaserieDto) =
        webClient.post()
            .uri("/fakturaserier")
            .bodyValue(fakturaserieDto)
            .retrieve()
            .bodyToMono<FaktureringskomponentResponse>()
            .block()!!
}
