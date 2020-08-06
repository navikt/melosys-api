package no.nav.melosys.tjenester.gui.unntakshandtering;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.exception.*;
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

    private static final String FEIL_OPPSTÅTT = "Feil oppstått: ";

    @ExceptionHandler(value = IkkeFunnetException.class)
    public ResponseEntity<Map<String, Object>> håndter(IkkeFunnetException e) {
        return håndter(e, HttpStatus.NOT_FOUND, Level.WARN);
    }

    @ExceptionHandler(value = FunksjonellException.class)
    public ResponseEntity<Map<String, Object>> håndter(FunksjonellException e) {
        return håndter(e, HttpStatus.BAD_REQUEST, Level.WARN);
    }

    @ExceptionHandler(value = TekniskException.class)
    public ResponseEntity<Map<String, Object>> håndter(TekniskException e) {
        return håndter(e, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR);
    }

    @ExceptionHandler(value = SikkerhetsbegrensningException.class)
    public ResponseEntity<Map<String, Object>> håndter(SikkerhetsbegrensningException e) {
        return håndter(e, HttpStatus.FORBIDDEN, Level.WARN);
    }

    @ExceptionHandler(value = ValideringException.class)
    public ResponseEntity<Map<String, Object>> håndter(ValideringException e) {
        return håndter(e, HttpStatus.BAD_REQUEST, Level.INFO, e.getFeilkoder());
    }

    private ResponseEntity<Map<String, Object>> håndter(Exception e, HttpStatus httpStatus, Level loggnivå){
        return håndter(e, httpStatus, loggnivå, Collections.emptyList());
    }

    private ResponseEntity<Map<String, Object>> håndter(Exception e,
                                                        HttpStatus httpStatus,
                                                        Level loggnivå,
                                                        Collection<String> begrunnelser) {
        if (loggnivå.equals(Level.ERROR)) {
            log.error(FEIL_OPPSTÅTT, e);
        } else if (loggnivå.equals(Level.WARN)) {
            log.warn(FEIL_OPPSTÅTT, e);
        }

        Map<String, Object> entity = new HashMap<>();
        entity.put("status", httpStatus.value());
        entity.put("error", httpStatus.getReasonPhrase());
        entity.put("message", e.getMessage());
        if (begrunnelser != null && !begrunnelser.isEmpty()) {
            entity.put("feilkoder", begrunnelser);
        }
        return new ResponseEntity<>(entity, httpStatus);
    }
}
