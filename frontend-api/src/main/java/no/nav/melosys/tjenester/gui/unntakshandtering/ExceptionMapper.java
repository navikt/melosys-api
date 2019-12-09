package no.nav.melosys.tjenester.gui.unntakshandtering;

import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionMapper {

    private static final Logger log = LoggerFactory.getLogger(ExceptionMapper.class);

    @ExceptionHandler(value = IkkeFunnetException.class)
    public ResponseEntity håndter(IkkeFunnetException e) {
        return håndter(e, HttpStatus.NOT_FOUND, Level.WARN);
    }

    @ExceptionHandler(value = FunksjonellException.class)
    public ResponseEntity håndter(FunksjonellException e) {
        return håndter(e, HttpStatus.BAD_REQUEST, Level.ERROR);
    }

    @ExceptionHandler(value = TekniskException.class)
    public ResponseEntity håndter(TekniskException e) {
        return håndter(e, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR);
    }

    @ExceptionHandler(value = SikkerhetsbegrensningException.class)
    public ResponseEntity håndter(SikkerhetsbegrensningException e) {
        return håndter(e, HttpStatus.FORBIDDEN, Level.WARN);
    }

    private ResponseEntity håndter(Exception e, HttpStatus httpStatus, Level loggnivå) {
        if (loggnivå.equals(Level.ERROR)) {
            log.error("{}", e.getMessage());
        } else if (loggnivå.equals(Level.WARN)) {
            log.warn("{}", e.getMessage());
        }

        Map<String, Object> entity = new HashMap<>();
        entity.put("status", httpStatus.value());
        entity.put("error", httpStatus.getReasonPhrase());
        entity.put("message", e.getMessage());
        return new ResponseEntity<>(entity, httpStatus);
    }
}
