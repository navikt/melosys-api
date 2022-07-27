package no.nav.melosys.exception;

import java.util.Collection;

import no.nav.melosys.exception.validering.KontrollfeilDto;

public class ValideringException extends RuntimeException {

    private final Collection<KontrollfeilDto> feilkoder;

    public ValideringException(String message, Collection<KontrollfeilDto> feilkoder) {
        super(message);
        this.feilkoder = feilkoder;
    }

    public Collection<KontrollfeilDto> getFeilkoder() {
        return feilkoder;
    }
}
