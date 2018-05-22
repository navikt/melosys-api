package no.nav.melosys.exception;

public class TekniskException extends RuntimeException {

    public TekniskException(String message) {
        super(message);
    }

    public TekniskException(Throwable throwable) {
        super(throwable);
    }

}
