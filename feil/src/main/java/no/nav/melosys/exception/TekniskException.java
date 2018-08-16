package no.nav.melosys.exception;

public class TekniskException extends MelosysException {

    public TekniskException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public TekniskException(String message) {
        super(message);
    }

    public TekniskException(Throwable throwable) {
        super(throwable);
    }

}
