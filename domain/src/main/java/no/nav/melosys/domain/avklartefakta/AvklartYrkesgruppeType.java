package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.exception.TekniskException;

public enum AvklartYrkesgruppeType {
    ORDINAER,
    FLYENDE_PERSONELL,
    SOKKEL_ELLER_SKIP,
    IKKE_YRKESAKTIV,
    KONTANTYTELSEMOTTAKER;

    public YrkesgruppeType tilYrkesgruppeType() throws TekniskException {
        switch(this) {
            case ORDINAER:
                return YrkesgruppeType.ORDINAER;
            case FLYENDE_PERSONELL:
                return YrkesgruppeType.FLYENDE_PERSONELL;
            case SOKKEL_ELLER_SKIP:
                return YrkesgruppeType.SOKKEL_ELLER_SKIP;
            default:
                throw new TekniskException("Finner ingen yrkesgruppe fra avklarte fakta");
        }
    }
}
