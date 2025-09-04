package no.nav.melosys.sikkerhet.context;

import java.util.ArrayList;
import java.util.List;

public class TestSubjectHandler extends SubjectHandler {

    @Override
    public String getOidcTokenString() {
        return null;
    }

    @Override
    public String getUserID() {
        return "Z990007";
    }

    @Override
    public String getUserName() {
        return "Testy test";
    }

    @Override
    public List<String> getGroups() {
        return new ArrayList<>();
    }

}
