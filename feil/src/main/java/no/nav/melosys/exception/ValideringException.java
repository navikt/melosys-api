package no.nav.melosys.exception;

import no.nav.melosys.exception.validering.KontrollfeilDto;

import java.util.Collection;

public class ValideringException extends MelosysException {

    private final Collection<KontrollfeilDto> feilkoder;

    public ValideringException(String message, Collection<KontrollfeilDto> feilkoder) {
        super(message);
        this.feilkoder = feilkoder;
    }

    public Collection<KontrollfeilDto> getFeilkoder() {
        return feilkoder;
    }
}
