package no.nav.melosys.integrasjon.ereg.organisasjon

import no.nav.melosys.exception.TekniskException
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

open class OrganisasjonRestConsumer(private val webClient: WebClient) {

    fun hentOrganisasjon(inntektRequest: OrganisasjonRequest): OrganisasjonResponse.Organisasjon {
        return webClient.get().uri("/organisasjon/{orgnummer}", inntektRequest.orgnummer)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<OrganisasjonResponse.Organisasjon>()
            .block() ?: throw TekniskException("Ereg organisasjon Response er null")
    }
}
