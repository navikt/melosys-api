package no.nav.melosys.tjenester.gui.unntakshandtering;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.ValideringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.CORRELATION_ID;

@ControllerAdvice
public class ExceptionMapper {

    private static final Logger log = LoggerFactory.getLogger(ExceptionMapper.class);

    @ExceptionHandler(value = IkkeFunnetException.class)
    public ResponseEntity<Map<String, Object>> håndter(IkkeFunnetException e, HttpServletRequest request) {
        return håndter(e, request, HttpStatus.NOT_FOUND, Level.WARN);
    }

    @ExceptionHandler(value = FunksjonellException.class)
    public ResponseEntity<Map<String, Object>> håndter(FunksjonellException e, HttpServletRequest request) {
        return håndter(e, request, HttpStatus.BAD_REQUEST, Level.WARN);
    }

    @ExceptionHandler(value = SikkerhetsbegrensningException.class)
    public ResponseEntity<Map<String, Object>> håndter(SikkerhetsbegrensningException e, HttpServletRequest request) {
        return håndter(e, request, HttpStatus.FORBIDDEN, Level.WARN);
    }

    @ExceptionHandler(value = ValideringException.class)
    public ResponseEntity<Map<String, Object>> håndter(ValideringException e, HttpServletRequest request) {
        return håndter(e, request, HttpStatus.BAD_REQUEST, Level.INFO, e.getFeilkoder());
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Map<String, Object>> håndter(Exception e, HttpServletRequest request) {
        return håndter(e, request, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR);
    }

    //    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Map<String, Object>> håndter(Exception e) {
        return håndter(e, null, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR);
    }

    private ResponseEntity<Map<String, Object>> håndter(Exception e, HttpServletRequest request, HttpStatus httpStatus, Level loggnivå) {
        return håndter(e, request, httpStatus, loggnivå, Collections.emptyList());
    }

    private ResponseEntity<Map<String, Object>> håndter(Exception e,
                                                        HttpServletRequest request,
                                                        HttpStatus httpStatus,
                                                        Level loggnivå,
                                                        Collection begrunnelser) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (loggnivå.equals(Level.ERROR)) {
            log.error("API kall feilet: {}\nremoteHost:{}\nrequestURI :{}", message, request.getRemoteHost(), request.getRequestURI(), e);
        } else if (loggnivå.equals(Level.WARN)) {
            log.warn("API kall feilet: {}\nremoteHost:{}\nrequestURI :{}", message, request.getRemoteHost(), request.getRequestURI(), e);
        }

        Map<String, Object> entity = new HashMap<>();
        entity.put("status", httpStatus.value());
        entity.put("error", httpStatus.getReasonPhrase());
        entity.put("message", message);
        entity.put("correlationId", MDC.get(CORRELATION_ID));
        if (begrunnelser != null && !begrunnelser.isEmpty()) {
            entity.put("feilkoder", begrunnelser);
        }
        return new ResponseEntity<>(entity, httpStatus);
    }
}
