package no.nav.melosys.integrasjon.dokgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;

@Retryable
public class DokgenConsumer {
    private static final Logger log = LoggerFactory.getLogger(DokgenConsumer.class);

    private final WebClient webClient;

    public DokgenConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public byte[] lagPdf(String malNavn, DokgenDto dokgenDto, boolean bestillKopi, boolean bestillUtkast) {
        log.info("Produserer PDF i melosys-dokgen. Mal: {}, som kopi {}", malNavn, bestillKopi);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Kun for testing husk å fjern
        try {
            String value = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dokgenDto);
            System.out.printf(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return webClient.post()
            .uri("/mal/{malNavn}/lag-pdf?somKopi={bestillKopi}&utkast={bestillUtkast}", malNavn, bestillKopi, bestillUtkast)
            .bodyValue(dokgenDto)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }
}
