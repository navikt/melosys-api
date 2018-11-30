package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.exception.TekniskException;

public enum AvklartYrkesgruppeType {
    YRKESAKTIV,
    YRKESAKTIV_FLYVENDE,
    YRKESAKTIV_SKIP,
    IKKE_YRKESAKTIV,
    KONTANTYTELSEMOTTAKER;

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
