package no.nav.melosys.exception;

public class SikkerhetsbegrensningException extends FunksjonellException {

    public SikkerhetsbegrensningException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public SikkerhetsbegrensningException(String message) {
        super(message);
    }

    public SikkerhetsbegrensningException(Throwable throwable) {
        super(throwable);
    }

}
