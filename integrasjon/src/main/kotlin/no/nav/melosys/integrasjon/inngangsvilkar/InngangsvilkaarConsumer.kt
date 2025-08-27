package no.nav.melosys.integrasjon.inngangsvilkar

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class InngangsvilkaarConsumer(private val inngangsvilkaarWebClient: WebClient) {

    fun vurderInngangsvilkår(
        brukersStatsborgerskap: Set<Land>,
        søknadsland: Set<String>,
        flereLandUkjentHvilke: Boolean,
        søknadsperiode: ErPeriode
    ): InngangsvilkarResponse {
        val request = VurderInngangsvilkaarRequest(
            brukersStatsborgerskap.map { it.kode }.toSet(),
            søknadsland,
            flereLandUkjentHvilke,
            søknadsperiode
        )

        return inngangsvilkaarWebClient.post()
            .uri("/inngangsvilkaar")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(InngangsvilkarResponse::class.java)
            .block() ?: error("Fikk uforventet null-respons fra inngangsvilkår")
    }
}
