package no.nav.melosys.sikkerhet.context;

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;

public abstract class SubjectHandler {

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

}
