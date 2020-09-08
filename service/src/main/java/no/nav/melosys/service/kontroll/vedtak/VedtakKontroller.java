package no.nav.melosys.service.kontroll.vedtak;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.kontroll.MedlemskapKontroller;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.validering.Kontrollfeil;

import java.util.ArrayList;
import java.util.List;

final class VedtakKontroller {

    private VedtakKontroller() {}

    public static final String ARBEID_UTLAND_NAVN = "arbeidUtland[%d].foretakNavn";
    public static final String ARBEID_UTLAND_LAND = "arbeidUtland[%d].adresse.landkode";
    public static final String FORETAK_UTLAND_NAVN = "foretakUtland[%d].navn";
    public static final String FORETAK_UTLAND_LAND = "foretakUtland[%d].adresse.landkode";

    static Kontrollfeil bostedsadresseForA1(VedtakKontrollData kontrollData) {
        return PersonKontroller.harRegistrertBostedsadresse(kontrollData.getPersonDokument(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_BOSTEDSADRESSE);
    }

    static Kontrollfeil overlappendeMedlemsperiode(VedtakKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(), medlemskapDokument)
            ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER) : null;
    }

    static Kontrollfeil periodeOver24Mnd(VedtakKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            && PeriodeKontroller.periodeOver24Mnd(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) : null;
    }

    static Kontrollfeil periodeManglerSluttdato(VedtakKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getTom() == null ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil arbeidsstedManglerFelter(VedtakKontrollData kontrollData) {
        List<ArbeidUtland> arbeidUtlandListe = kontrollData.getBehandlingsgrunnlagData().arbeidUtland;
        List<String> felter = new ArrayList<>();

        for (int i = 0; i < arbeidUtlandListe.size(); i++) {
            ArbeidUtland arbeidUtland = arbeidUtlandListe.get(i);
            if (arbeidUtland.foretakNavn == null) {
                felter.add(String.format(ARBEID_UTLAND_NAVN, i));
            }
            if (arbeidUtland.adresse.landkode == null) {
                felter.add(String.format(ARBEID_UTLAND_LAND, i));
            }
        }
        return felter.size() == 0 ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED, felter);
    }

    static Kontrollfeil foretakUtlandManglerFelter(VedtakKontrollData kontrollData) {
        List<ForetakUtland> foretakUtlandListe = kontrollData.getBehandlingsgrunnlagData().foretakUtland;
        List<String> felter = new ArrayList<>();

        for (int i = 0; i < foretakUtlandListe.size(); i++) {
            ForetakUtland foretakUtland = foretakUtlandListe.get(i);
            if (foretakUtland.navn == null) {
                felter.add(String.format(FORETAK_UTLAND_NAVN, i));
            }
            if (foretakUtland.adresse.landkode == null) {
                felter.add(String.format(FORETAK_UTLAND_LAND, i));
            }
        }
        return felter.size() == 0 ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL, felter);
    }
}
