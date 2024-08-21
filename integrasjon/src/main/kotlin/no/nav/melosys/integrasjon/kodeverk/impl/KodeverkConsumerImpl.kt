package no.nav.melosys.integrasjon.kodeverk.impl

import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.kodeverk.impl.dto.FellesKodeverkDto
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.util.*

class KodeverkConsumerImpl internal constructor(private val webClient: WebClient) : CallIdAware {

    fun hentKodeverk(navn: String): FellesKodeverkDto {
        val requestId = UUID.randomUUID();
        val kodeverkRequest = webClient
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

        ThreadLocalAccessInfo.beforeExecuteProcess(requestId, "hent felles-kodeverk")
        val fellesKodeverkDto = kodeverkRequest.block() ?: throw RuntimeException("Feilet å hente felles-kodeverk")
        ThreadLocalAccessInfo.afterExecuteProcess(requestId)

        return fellesKodeverkDto
    }

    companion object {
        private const val VERSJON = "v1"
    }
}
