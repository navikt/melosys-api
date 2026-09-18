package no.nav.melosys.exception;

/**
 * Kastes når en ekstern integrasjon har mottatt og behandlet en forespørsel, men avvist den
 * som varig ugyldig (f.eks. en valideringsfeil videreformidlet fra melosys-eessi/eux-rina-api).
 * Et nytt forsøk med samme data vil alltid gi samme resultat, og bør derfor ikke retry'es
 * (se {@code @Retryable(noRetryFor = ...)} på aktuell klient).
 */
public class IkkeRetrybarIntegrasjonException extends IntegrasjonException {

    public IkkeRetrybarIntegrasjonException(String message) {
        super(message);
    }

    public IkkeRetrybarIntegrasjonException(Throwable throwable) {
        super(throwable);
    }

    public IkkeRetrybarIntegrasjonException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
