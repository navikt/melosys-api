package no.nav.melosys.exception;

import java.util.Collection;

public class ValideringException extends MelosysException {

    private final Collection<String> feilkoder;

    public ValideringException(String message, Collection<String> feilkoder) {
        super(message);
        this.feilkoder = feilkoder;
    }

    public Collection<String> getFeilkoder() {
        return feilkoder;
    }
}
