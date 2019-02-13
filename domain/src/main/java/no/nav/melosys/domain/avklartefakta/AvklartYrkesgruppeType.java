package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.TekniskException;

public enum AvklartYrkesgruppeType {
    ORDINAER,
    FLYENDE_PERSONELL,
    SOKKEL_ELLER_SKIP,
    IKKE_YRKESAKTIV,
    KONTANTYTELSEMOTTAKER;

    public Yrkesgrupper tilYrkesgruppeType() throws TekniskException {
        switch(this) {
            case ORDINAER:
                return Yrkesgrupper.ORDINAER;
            case FLYENDE_PERSONELL:
                return Yrkesgrupper.FLYENDE_PERSONELL;
            case SOKKEL_ELLER_SKIP:
                return Yrkesgrupper.SOKKEL_ELLER_SKIP;
            default:
                throw new TekniskException("Finner ingen yrkesgruppe fra avklarte fakta");
        }
    }
}
