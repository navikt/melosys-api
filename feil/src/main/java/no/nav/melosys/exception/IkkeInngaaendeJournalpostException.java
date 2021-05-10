package no.nav.melosys.exception;

public class IkkeInngaaendeJournalpostException extends RuntimeException {

    public IkkeInngaaendeJournalpostException(String message) {
        super(message);
    }

    public IkkeInngaaendeJournalpostException(Throwable t) {
        super(t);
    }

    public IkkeInngaaendeJournalpostException(String message, Throwable t) {
        super(message, t);
    }

}
