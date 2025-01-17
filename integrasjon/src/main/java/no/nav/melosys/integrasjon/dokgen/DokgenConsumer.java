package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.StandardvedleggDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;

//TODO: Kotlinize
@Retryable
public class DokgenConsumer {
    private static final Logger log = LoggerFactory.getLogger(DokgenConsumer.class);

    private final WebClient webClient;

    public DokgenConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public byte[] lagPdf(String malNavn, DokgenDto dokgenDto, boolean bestillKopi, boolean bestillUtkast) {
        log.info("Produserer PDF i melosys-dokgen. Mal: {}, som kopi {}", malNavn, bestillKopi);
        return webClient.post()
            .uri("/mal/{malNavn}/lag-pdf?somKopi={bestillKopi}&utkast={bestillUtkast}", malNavn, bestillKopi, bestillUtkast)
            .bodyValue(dokgenDto)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }

    public byte[] lagPdfForStandardvedlegg(String malNavn, StandardvedleggDto standardvedlegg) {
        log.info("Produserer standardvedlegg-PDF i melosys-dokgen. Mal: {}", malNavn);
        return webClient.post()
            .uri("/mal/{malNavn}/lag-pdf?somKopi=false&utkast=false", malNavn)
            .bodyValue(standardvedlegg)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }
}
