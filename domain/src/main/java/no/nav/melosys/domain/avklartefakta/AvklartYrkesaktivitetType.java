package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.exception.TekniskException;

public enum AvklartYrkesaktivitetType {
    YRKESAKTIV("YRKESAKTIV") {
        @Override
        public YrkesgruppeType tilYrkesgruppeType() {
            return YrkesgruppeType.ORDINAER;
        }
    },
    YRKESAKTIV_FLYVENDE("YRKESAKTIV_FLYVENDE") {
        @Override
        public YrkesgruppeType tilYrkesgruppeType() {
            return YrkesgruppeType.FLYENDE_PERSONELL;
        }
    },
    YRKESAKTIV_SKIP("YRKESAKTIV_SKIP") {
        @Override
        public YrkesgruppeType tilYrkesgruppeType() {
            return YrkesgruppeType.SOKKEL_ELLER_SKIP;
        }
    },
    IKKE_YRKESAKTIV("IKKE_YRKESAKTIV"),
    KONTANTYTELSEMOTTAKER("KONTANTYTELSEMOTTAKER");

    public String navn;

    AvklartYrkesaktivitetType(String navn) {
        this.navn = navn;
    }

    public String value() {
        return navn;
    }

    public YrkesgruppeType tilYrkesgruppeType() throws TekniskException {
        throw new TekniskException("Finner ingen yrkesgruppe fra avklarte fakta");
    }
}
