package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.util.Assert;

public enum FastMottakerMedOrgnr {
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

    public static boolean orgNrErSkatt(String orgnr) {
        return OrgNr.SKATTEETATEN_ORGNR.orgnr.equals(orgnr);
    }

    public static Mottaker av(FastMottakerMedOrgnr mottaker) {
        Assert.notNull(mottaker, "FastMottakerMedOrgnr trengs.");
        return switch (mottaker) {
            case HELFO -> Mottaker.av(lagAktør(OrgNr.HELFO_ORGNR));
            case SKATT -> Mottaker.av(lagAktør(OrgNr.SKATTEETATEN_ORGNR));
            case STATLIG_SKATTEOPPKREVING -> Mottaker.av(lagAktør(OrgNr.STATLIG_SKATTEOPPKREVING_ORGNR));
            default -> throw new IllegalArgumentException(mottaker + " støttes ikke.");
        };
    }

    private static Aktoer lagAktør(OrgNr orgn) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktør.setOrgnr(orgn.getOrgnr());
        return aktør;
    }
}
