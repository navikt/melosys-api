package no.nav.melosys.integrasjon.kodeverk.impl

import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.kodeverk.impl.dto.FellesKodeverkDto
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

class KodeverkConsumerImpl internal constructor(private val webClient: WebClient) : CallIdAware {

    fun hentKodeverk(navn: String): FellesKodeverkDto {
        return webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/$VERSJON/kodeverk/$navn/koder/betydninger")
                    .queryParam("ekskluderUgyldige", false)
                    .queryParam("oppslagsdato", LocalDate.MIN)
                    .queryParam("spraak", KodeverkRegisterImpl.BOKMÅL)
                    .build()
            }
            .retrieve()
            .bodyToMono(FellesKodeverkDto::class.java)
            .block() ?: throw RuntimeException("Feilet å hente felles-kodeverk")
    }

    companion object {
        private const val VERSJON = "v1"
    }
}
