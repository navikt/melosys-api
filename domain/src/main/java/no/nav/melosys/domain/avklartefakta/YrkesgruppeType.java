package no.nav.melosys.domain.avklartefakta;

public enum YrkesgruppeType {
    YRKESAKTIV("YRKESAKTIV"),
    YRKESAKTIV_FLYVENDE("YRKESAKTIV_FLYVENDE"),
    YRKESAKTIV_SKIP("YRKESAKTIV_SKIP"),
    IKKE_YRKESAKTIV("IKKE_YRKESAKTIV"),
    KONTANTYTELSEMOTTAKER("KONTANTYTELSEMOTTAKER");

    public String navn;

    YrkesgruppeType(String navn) {
        this.navn = navn;
    }

    public String value() {
        return navn;
    }
}
