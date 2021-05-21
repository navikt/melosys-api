package no.nav.melosys.exception;

public class FunksjonellException extends RuntimeException {

    public FunksjonellException(String s) {
        super(s);
    }

    public FunksjonellException(Throwable t) {
        super(t);
    }

    public FunksjonellException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
