package no.nav.melosys.sikkerhet.context;

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