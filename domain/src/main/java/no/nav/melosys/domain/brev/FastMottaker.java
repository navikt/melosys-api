package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.util.Assert;

public enum FastMottaker {
    HELFO,
    SKATT,
    STATLIG_SKATTEOPPKREVING;

    public enum OrgNr {
        HELFO_ORGNR("986965610"),
        SKATTEETATEN_ORGNR("974761076"),
        STATLIG_SKATTEOPPKREVING_ORGNR("992187298");

        private final String orgnr;

        OrgNr(String orgnr) {
            this.orgnr = orgnr;
        }

        public String getOrgnr() {
            return orgnr;
        }
    }

    public static Mottaker av(FastMottaker mottaker) {
        Assert.notNull(mottaker, "FastMottaker trengs.");
        return switch (mottaker) {
            case HELFO -> Mottaker.av(lagAktør(OrgNr.HELFO_ORGNR));
            case SKATT -> Mottaker.av(lagAktør(OrgNr.SKATTEETATEN_ORGNR));
            case STATLIG_SKATTEOPPKREVING -> Mottaker.av(lagAktør(OrgNr.STATLIG_SKATTEOPPKREVING_ORGNR));
            default -> throw new IllegalArgumentException(mottaker + " støttes ikke.");
        };
    }

    private static Aktoer lagAktør(OrgNr orgn) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.MYNDIGHET);
        aktør.setOrgnr(orgn.getOrgnr());
        return aktør;
    }
}
