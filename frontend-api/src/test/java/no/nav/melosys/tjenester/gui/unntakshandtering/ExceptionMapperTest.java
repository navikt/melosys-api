package no.nav.melosys.tjenester.gui.unntakshandtering;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExceptionMapperTest {

    private ExceptionMapper exceptionMapper;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setup() {
        exceptionMapper = new ExceptionMapper();
    }

    @Test
    void funksjonellException() {
        final String melding = "Funksjonell feil";
        FunksjonellException funksjonellException = new FunksjonellException(melding);
        assertResponse(exceptionMapper.håndter( funksjonellException, request), HttpStatus.BAD_REQUEST, melding);
    }

    @Test
    void tekniskException() {
        final String melding = "Teknisk feil";
        TekniskException funksjonellException = new TekniskException(melding);
        assertResponse(exceptionMapper.håndter(funksjonellException, request), HttpStatus.INTERNAL_SERVER_ERROR, melding);
    }

    @Test
    void jwtTokenUnauthorizedException_SkalLoggesSomWarn_ReturnerStatusForbidden() {
        JwtTokenUnauthorizedException jwtTokenUnauthorizedException = new JwtTokenUnauthorizedException();
        assertResponse(exceptionMapper.håndter(jwtTokenUnauthorizedException, request), HttpStatus.UNAUTHORIZED, "JwtTokenUnauthorizedException");
    }

    @Test
    void sikkerhetsBegrensningException() {
        final String melding = "Sikkerhetsfeil";
        SikkerhetsbegrensningException funksjonellException = new SikkerhetsbegrensningException(melding);
        assertResponse(exceptionMapper.håndter(funksjonellException, request), HttpStatus.FORBIDDEN, melding);
    }

    @Test
    void ikkeFunnetException() {
        final String melding = "Teknisk feil";
        IkkeFunnetException funksjonellException = new IkkeFunnetException(melding);
        assertResponse(exceptionMapper.håndter(funksjonellException, request), HttpStatus.NOT_FOUND, melding);
    }

    private void assertResponse(ResponseEntity<Map<String, Object>> responseEntity, HttpStatus forventetStatus, String forventetMelding) {
        assertThat(responseEntity.getStatusCode()).isEqualTo(forventetStatus);
        assertThat(responseEntity.getBody()).isInstanceOf(Map.class);
        Map<String, Object> body = responseEntity.getBody();
        assertThat(body).containsEntry("message", forventetMelding);
    }

}
