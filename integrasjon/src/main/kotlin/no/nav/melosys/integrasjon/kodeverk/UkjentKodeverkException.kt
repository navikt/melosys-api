package no.nav.melosys.integrasjon.kodeverk;

public class UkjentKodeverkException extends RuntimeException {
    
    public UkjentKodeverkException(String message) {
        super(message);
    }
    
    public UkjentKodeverkException(String message, Exception cause) {
        super(message, cause);
    }

}
