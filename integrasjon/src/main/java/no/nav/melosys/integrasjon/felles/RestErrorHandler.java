package no.nav.melosys.integrasjon.felles;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;

public abstract class RestErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(RestErrorHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RuntimeException tilException(String feilmelding, HttpStatusCode status) {
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            return new SikkerhetsbegrensningException(feilmelding);
        } else if (status == HttpStatus.NOT_FOUND) {
            return new IkkeFunnetException(feilmelding);
        } else if (status.is4xxClientError()) {
            return new FunksjonellException(feilmelding);
        } else {
            throw new TekniskException(feilmelding);
        }
    }

    public RuntimeException tilException(HttpStatusCodeException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String feilmelding = hentFeilmelding(e);
        return tilException(feilmelding, status);
    }

    private String hentFeilmelding(HttpStatusCodeException e) {
        String feilmelding = e.getResponseBodyAsString();
        if (!StringUtils.hasText(feilmelding)) return e.getMessage();
        try {
            JsonNode json = objectMapper.readTree(feilmelding).path("message");
            return json.isMissingNode() ? e.getMessage() : json.toString();
        } catch (IOException ex) {
            log.warn("Kunne ikke lese feilmelding fra response", ex);
            return feilmelding;
        }
    }
}
