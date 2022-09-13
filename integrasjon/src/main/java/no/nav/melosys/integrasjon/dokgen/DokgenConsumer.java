package no.nav.melosys.integrasjon.dokgen;

import io.netty.handler.codec.http.HttpHeaderNames;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.brev.FritekstvedleggBestilling;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.Fritekstbrev;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

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
}
