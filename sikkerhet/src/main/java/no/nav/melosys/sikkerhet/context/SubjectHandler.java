package no.nav.melosys.sikkerhet.context;

import java.util.List;

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;

public abstract class SubjectHandler {

    public static String SYSTEMBRUKER = "srvmelosys";

    private static class SubjectHandlerHolder {
        private static SubjectHandler SUBJECT_HANDLER = new SpringSubjectHandler(new SpringTokenValidationContextHolder());
    }

    public static SubjectHandler getInstance() {
        return SubjectHandlerHolder.SUBJECT_HANDLER;
    }

    public static void set(SubjectHandler subjectHandler) {
        SubjectHandlerHolder.SUBJECT_HANDLER = subjectHandler;
    }

    public abstract String getOidcTokenString();

    public abstract String getUserID();

    public abstract String getUserName();

    public abstract List<String> getGroups();

    /**
     * Verdien av {@code idtyp} i innkommende token, eller {@code null} hvis den mangler.
     * {@code "app"} betyr maskin-til-maskin-token.
     * Ikke abstrakt, slik at eksisterende subklasser (blant annet i tester) ikke må endres.
     */
    public String getTokenIdType() {
        return null;
    }

    public static String getSaksbehandlerIdent() {
        return getInstance().getUserID();
    }

    public static String getUserIDOrSystemUser() {
        return getInstance().getUserID() == null ? SYSTEMBRUKER : getInstance().getUserID();
    }

}
