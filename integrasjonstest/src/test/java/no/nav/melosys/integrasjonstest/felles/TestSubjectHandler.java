package no.nav.melosys.integrasjonstest.felles;

import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class TestSubjectHandler extends SubjectHandler {
    @Override
    public String getOidcTokenString() {
        return null;
    }

    @Override
    public String getUserID() {
        return "Z990007";
    }
}
