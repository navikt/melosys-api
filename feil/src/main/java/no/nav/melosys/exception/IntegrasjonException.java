package no.nav.melosys.exception;

public class IntegrasjonException extends TekniskException {

    public IntegrasjonException(String message) {
        super(message);
    }

    public IntegrasjonException(Throwable throwable) {
        super(throwable);
    }

    public IntegrasjonException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
