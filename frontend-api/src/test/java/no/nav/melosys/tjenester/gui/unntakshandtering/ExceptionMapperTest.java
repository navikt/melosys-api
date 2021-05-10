package no.nav.melosys.tjenester.gui.unntakshandtering;

import java.util.Map;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class ExceptionMapperTest {

    private ExceptionMapper exceptionMapper;

    @BeforeEach
    public void setup() {
        exceptionMapper = new ExceptionMapper();
    }

    @Test
    public void funksjonellException() {
        final String melding = "Funksjonell feil";
        FunksjonellException funksjonellException = new FunksjonellException(melding);
        assertResponse(exceptionMapper.håndter(funksjonellException), HttpStatus.BAD_REQUEST, melding);
    }

    @Test
    public void tekniskException() {
        final String melding = "Teknisk feil";
        TekniskException funksjonellException = new TekniskException(melding);
        assertResponse(exceptionMapper.håndter(funksjonellException), HttpStatus.INTERNAL_SERVER_ERROR, melding);
    }

    @Test
    public void sikkerhetsBegrensningException() {
        final String melding = "Sikkerhetsfeil";
        SikkerhetsbegrensningException funksjonellException = new SikkerhetsbegrensningException(melding);
        assertResponse(exceptionMapper.håndter(funksjonellException), HttpStatus.FORBIDDEN, melding);
    }

    @Test
    public void ikkeFunnetException() {
        final String melding = "Teknisk feil";
        IkkeFunnetException funksjonellException = new IkkeFunnetException(melding);
        assertResponse(exceptionMapper.håndter(funksjonellException), HttpStatus.NOT_FOUND, melding);
    }

    private void assertResponse(ResponseEntity responseEntity, HttpStatus forventetStatus, String forventetMelding) {
        assertThat(responseEntity.getStatusCode()).isEqualTo(forventetStatus);
        assertThat(responseEntity.getBody()).isInstanceOf(Map.class);
        Map<String, String> body = (Map<String, String>) responseEntity.getBody();
        assertThat(body.get("message")).isEqualTo(forventetMelding);
    }

}
