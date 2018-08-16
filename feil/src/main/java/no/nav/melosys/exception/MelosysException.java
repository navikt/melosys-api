package no.nav.melosys.exception;

public abstract class MelosysException extends Exception {

    public MelosysException(String message) {
        super(message);
    }

    public MelosysException(Throwable throwable) {
        super(throwable);
    }

    public MelosysException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
