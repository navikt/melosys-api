package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.dokgen.dto.Flettedata;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.DistribuerJournalpostConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;

@Component
public class DokgenConsumer {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpostConsumer.class);

    private final RestTemplate dokgenRestTemplate;

    @Autowired
    public DokgenConsumer(RestTemplate dokgenRestTemplate) {
        this.dokgenRestTemplate = dokgenRestTemplate;
    }


    public byte[] lagPdf(String mal, Flettedata flettedata) {
        log.info("Produserer PDF i melosys-dokgen. Mal: {}, flettedata: {}", mal, flettedata);
        String lagPdfUri = "/mal/%s/lag-pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return dokgenRestTemplate.postForObject(format(lagPdfUri, mal), new HttpEntity<>(flettedata, headers), byte[].class);
    }
}
