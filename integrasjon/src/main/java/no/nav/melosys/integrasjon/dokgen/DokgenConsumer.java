package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DokgenConsumer {
    private static final Logger log = LoggerFactory.getLogger(DokgenConsumer.class);

    private final WebClient webClient;

    public DokgenConsumer(@Value("${melosysdokgen.v1.url}") String url) {
        this.webClient = WebClient.create(url);
    }

    public byte[] lagPdf(String malNavn, DokgenDto dokgenDto) {
        log.info("Produserer PDF i melosys-dokgen. Mal: {}", malNavn);
        String lagPdfUri = "/mal/{malNavn}/lag-pdf";

        return webClient.post().uri(lagPdfUri, malNavn)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dokgenDto)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }
}
