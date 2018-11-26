package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.exception.TekniskException;

public enum AvklartYrkesgruppeType {
    YRKESAKTIV("YRKESAKTIV"),
    YRKESAKTIV_FLYVENDE("YRKESAKTIV_FLYVENDE"),
    YRKESAKTIV_SKIP("YRKESAKTIV_SKIP"),
    IKKE_YRKESAKTIV("IKKE_YRKESAKTIV"),
    KONTANTYTELSEMOTTAKER("KONTANTYTELSEMOTTAKER");

    public String navn;

    AvklartYrkesgruppeType(String navn) {
        this.navn = navn;
    }

    public String value() {
        return navn;
    }

    public YrkesgruppeType tilYrkesgruppeType() throws TekniskException {
        switch(this) {
            case YRKESAKTIV:
                return YrkesgruppeType.ORDINAER;
            case YRKESAKTIV_FLYVENDE:
                return YrkesgruppeType.FLYENDE_PERSONELL;
            case YRKESAKTIV_SKIP:
                return YrkesgruppeType.SOKKEL_ELLER_SKIP;
            default:
                throw new TekniskException("Finner ingen yrkesgruppe fra avklarte fakta");
        }
    }
}
