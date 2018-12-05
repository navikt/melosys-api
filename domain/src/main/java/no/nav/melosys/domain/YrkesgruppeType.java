package no.nav.melosys.domain;

public enum YrkesgruppeType {
    ORDINAER("ORDINAER"),
    FLYENDE_PERSONELL("FLYENDE_PERSONELL"),
    SOKKEL_ELLER_SKIP("SOKKEL_ELLER_SKIP");

    private String navn;

    YrkesgruppeType(String navn) {
        this.navn = navn;
    }

    public String value() {
        return navn;
    }
}
