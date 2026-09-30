package no.nav.melosys.integrasjon.dokgen

import mu.KotlinLogging
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.StandardvedleggDto
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger {}

@Retryable
open class DokgenClient(private val webClient: WebClient) {

    open fun lagPdf(
        malNavn: String,
        dokgenDto: DokgenDto,
        bestillKopi: Boolean,
        bestillUtkast: Boolean
    ): ByteArray? {
        log.info { "Produserer PDF i melosys-dokgen. Mal: $malNavn, som kopi $bestillKopi" }
        return webClient.post()
            .uri("/mal/{malNavn}/lag-pdf?somKopi={bestillKopi}&utkast={bestillUtkast}", malNavn, bestillKopi, bestillUtkast)
            .bodyValue(dokgenDto)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block()
    }

    open fun lagPdfForStandardvedlegg(malNavn: String, standardvedlegg: StandardvedleggDto?): ByteArray? {
        log.info { "Produserer standardvedlegg-PDF i melosys-dokgen. Mal: $malNavn" }
        val request = webClient.post()
            .uri("/mal/{malNavn}/lag-pdf?somKopi=false&utkast=false", malNavn)

        return if (standardvedlegg != null) {
            request.bodyValue(standardvedlegg)
                .retrieve()
                .bodyToMono<ByteArray>()
                .block()
        } else {
            request.header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono<ByteArray>()
                .block()
        }
    }
}
