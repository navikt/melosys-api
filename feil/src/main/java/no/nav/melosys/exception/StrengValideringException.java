package no.nav.melosys.exception;

import java.util.Collection;

import no.nav.melosys.exception.validering.KontrollfeilDto;

public class StrengValideringException extends Exception {

    private final Collection<KontrollfeilDto> feilkoder;

    public StrengValideringException(String message, Collection<KontrollfeilDto> feilkoder) {
        super(message);
        this.feilkoder = feilkoder;
    }

    public Collection<KontrollfeilDto> getFeilkoder() {
        return feilkoder;
    }
}
