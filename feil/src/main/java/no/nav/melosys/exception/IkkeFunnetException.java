package no.nav.melosys.exception;

public class IkkeFunnetException extends FunksjonellException {

    public IkkeFunnetException(String message) {
        super(message);
    }

    public IkkeFunnetException(Throwable t) {
        super(t);
    }

    public IkkeFunnetException(String message, Throwable t) {
        super(message, t);
    }

}
