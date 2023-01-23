package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.util.Assert;

public enum FastMottakerMedOrgnr {
    HELFO,
    SKATTEETATEN,
    SKATTEINNKREVER_UTLAND;

    public String getOrgnr() {
        return switch (this) {
            case HELFO -> Etat.HELFO_ORGNR.getOrgnr();
            case SKATTEETATEN -> Etat.SKATTEETATEN_ORGNR.getOrgnr();
            case SKATTEINNKREVER_UTLAND -> Etat.SKATTINNKREVER_UTLAND_ORGNR.getOrgnr();
        };
    }

    public static Mottaker av(FastMottakerMedOrgnr mottaker) {
        Assert.notNull(mottaker, "FastMottakerMedOrgnr trengs.");
        return switch (mottaker) {
            case HELFO -> Mottaker.av(lagAktør(Etat.HELFO_ORGNR));
            case SKATTEETATEN -> Mottaker.av(lagAktør(Etat.SKATTEETATEN_ORGNR));
            case SKATTEINNKREVER_UTLAND -> Mottaker.av(lagAktør(Etat.SKATTINNKREVER_UTLAND_ORGNR));
        };
    }

    private static Aktoer lagAktør(Etat orgn) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktør.setOrgnr(orgn.getOrgnr());
        return aktør;
    }
}
