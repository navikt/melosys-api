package no.nav.melosys.exception;

import no.nav.melosys.exception.validering.FeilKode;

import java.util.Collection;

public class ValideringException extends MelosysException {

    private final Collection<FeilKode> feilkoder;

    public ValideringException(String message, Collection<FeilKode> feilkoder) {
        super(message);
        this.feilkoder = feilkoder;
    }

    public Collection<FeilKode> getFeilkoder() {
        return feilkoder;
    }
}
