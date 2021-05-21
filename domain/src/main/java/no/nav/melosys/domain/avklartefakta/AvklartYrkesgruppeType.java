package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.TekniskException;

public enum AvklartYrkesgruppeType {
    ORDINAER,
    ORDINAER_UTEN_ART12,
    YRKESAKTIV_FLYVENDE,
    SOKKEL_ELLER_SKIP,
    IKKE_YRKESAKTIV,
    KONTANTYTELSEMOTTAKER;

    public Yrkesgrupper tilYrkesgruppeType() {
        switch(this) {
            case ORDINAER:
            case ORDINAER_UTEN_ART12:
                return Yrkesgrupper.ORDINAER;
            case YRKESAKTIV_FLYVENDE:
                return Yrkesgrupper.FLYENDE_PERSONELL;
            case SOKKEL_ELLER_SKIP:
                return Yrkesgrupper.SOKKEL_ELLER_SKIP;
            default:
                throw new TekniskException("Finner ingen yrkesgruppe fra avklarte fakta");
        }
    }
}
