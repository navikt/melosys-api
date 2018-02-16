package no.nav.melosys.integrasjon.felles.exception;

public class IntegrasjonException extends RuntimeException {

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
