package no.nav.melosys.saksflyt.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.util.Assert;

public enum FastMottaker {
    HELFO,
    SKATT;

    private static final String HELFO_ORGNR = "986965610";
    private static final String SKATTEETATEN_ORGNR = "974761076";

    public static Mottaker av(FastMottaker mottaker) {
        Assert.notNull(mottaker, "FastMottaker trengs.");
        switch (mottaker) {
            case HELFO: return Mottaker.av(lagAktør(HELFO_ORGNR));
            case SKATT: return Mottaker.av(lagAktør(SKATTEETATEN_ORGNR));
            default: throw new IllegalArgumentException(mottaker + " støttes ikke.");
        }
    }

    private static Aktoer lagAktør(String orgn) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.MYNDIGHET);
        aktør.setOrgnr(orgn);
        return aktør;
    }
}
