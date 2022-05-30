package no.nav.melosys.service.ferdigbehandling.kontroll;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.service.arbeidutland.kontroll.ArbeidUtlandKontrollService;
import no.nav.melosys.service.ferdigbehandling.kontroll.FerdigbehandlingKontrollData;
import no.nav.melosys.service.kontroll.ArbeidsstedKontroller;
import no.nav.melosys.service.kontroll.OverlappendeMedlemskapsperioderKontroller;
import no.nav.melosys.service.kontroll.PeriodeKontroller;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.validering.Kontrollfeil;

final class FerdigbehandlingKontroller {

    private FerdigbehandlingKontroller() {
    }

    static Kontrollfeil overlappendeMedlemsperiode(FerdigbehandlingKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();
        Lovvalgsperiode opprinneligLovvalgsperiode = kontrollData.getOpprinneligLovvalgsperiode();

        return OverlappendeMedlemskapsperioderKontroller.harOverlappendeMedlemsperiode(medlemskapDokument,
            lovvalgsperiode, opprinneligLovvalgsperiode) ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER) : null;
    }

    static Kontrollfeil periodeOver24Mnd(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.erArtikkel12()
            && PeriodeKontroller.periodeOver24Måneder(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) : null;
    }

    static Kontrollfeil periodeManglerSluttdato(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getTom() == null ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil periodeOverTreÅr(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1
            && PeriodeKontroller.periodeOver3År(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_TRE_ÅR) : null;
    }

    static Kontrollfeil arbeidsstedManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontrollService.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil foretakUtlandManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontrollService.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil representantIUtlandetMangler(FerdigbehandlingKontrollData kontrollData) {
        var lovvalgsperiode = kontrollData.getLovvalgsperiode();
        var behandlingsgrunnlagData = (SoeknadTrygdeavtale) kontrollData.getBehandlingsgrunnlagData();

        return erBestemmelseDerTrygdeavtaleAttestSendes(lovvalgsperiode.getBestemmelse())
            && ArbeidsstedKontroller.representantIUtlandetMangler(behandlingsgrunnlagData.getRepresentantIUtlandet())
            ? new Kontrollfeil(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED) : null;
    }

    static Kontrollfeil adresseRegistrert(FerdigbehandlingKontrollData kontrollData) {
        return PersonKontroller.harRegistrertAdresse(kontrollData.getPersondata(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendes(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1
            || bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_5
            || bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART7_3;
    }
}
