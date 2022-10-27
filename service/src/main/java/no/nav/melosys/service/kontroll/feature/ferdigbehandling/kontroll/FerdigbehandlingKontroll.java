package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.kontroll.regler.ArbeidsstedRegler;
import no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.kontroll.regler.PersonRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_usa.*;

final class FerdigbehandlingKontroll {

    private FerdigbehandlingKontroll() {
    }

    static Kontrollfeil overlappendeMedlemsperiode(FerdigbehandlingKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.medlemskapDokument();
        PeriodeOmLovvalg lovvalgsperiode = kontrollData.lovvalgsperiode();
        Lovvalgsperiode opprinneligLovvalgsperiode = kontrollData.opprinneligLovvalgsperiode();

        return OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(medlemskapDokument,
            lovvalgsperiode, opprinneligLovvalgsperiode) ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER) : null;
    }

    static Kontrollfeil periodeOver24Mnd(FerdigbehandlingKontrollData kontrollData) {
        PeriodeOmLovvalg lovvalgsperiode = kontrollData.lovvalgsperiode();

        return lovvalgsperiode.erArtikkel12()
            && PeriodeRegler.periodeOver24Måneder(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) : null;
    }

    static Kontrollfeil periodeManglerSluttdato(FerdigbehandlingKontrollData kontrollData) {
        PeriodeOmLovvalg lovvalgsperiode = kontrollData.lovvalgsperiode();

        return lovvalgsperiode.getTom() == null ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil periodeOverTreÅr(FerdigbehandlingKontrollData kontrollData) {
        PeriodeOmLovvalg lovvalgsperiode = kontrollData.lovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == UK_ART6_1
            && PeriodeRegler.periodeOver3År(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_TRE_ÅR) : null;
    }

    static Kontrollfeil periodeOverFemÅr(FerdigbehandlingKontrollData kontrollData) {
        PeriodeOmLovvalg lovvalgsperiode = kontrollData.lovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == USA_ART5_2
            && PeriodeRegler.periodeOver5År(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_FEM_ÅR) : null;
    }

    static Kontrollfeil periodeOver12Måneder(FerdigbehandlingKontrollData kontrollData) {
        PeriodeOmLovvalg lovvalgsperiode = kontrollData.lovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == USA_ART5_4
            && PeriodeRegler.periodeOver12Måneder(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_12_MD) : null;
    }

    static Kontrollfeil arbeidsstedManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.arbeidsstedManglerFelter(kontrollData.behandlingsgrunnlagData());
    }

    static Kontrollfeil foretakUtlandManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroll.foretakUtlandManglerFelter(kontrollData.behandlingsgrunnlagData());
    }

    static Kontrollfeil representantIUtlandetMangler(FerdigbehandlingKontrollData kontrollData) {
        var lovvalgsperiode = kontrollData.lovvalgsperiode();
        var behandlingsgrunnlagData = (SoeknadTrygdeavtale) kontrollData.behandlingsgrunnlagData();

        return erBestemmelseDerTrygdeavtaleAttestSendes(lovvalgsperiode.getBestemmelse())
            && ArbeidsstedRegler.representantIUtlandetMangler(behandlingsgrunnlagData.getRepresentantIUtlandet())
            ? new Kontrollfeil(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED) : null;
    }

    static Kontrollfeil adresseRegistrert(FerdigbehandlingKontrollData kontrollData) {
        return PersonRegler.harRegistrertAdresse(kontrollData.persondata(), kontrollData.behandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendes(LovvalgBestemmelse bestemmelse) {
        return erBestemmelseDerTrygdeavtaleAttestSendesUK(bestemmelse) || erBestemmelseDerTrygdeavtaleAttestSendesUSA(bestemmelse);
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendesUK(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1
            || bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_5
            || bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART7_3;
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendesUSA(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == USA_ART5_2
            || bestemmelse == USA_ART5_4
            || bestemmelse == USA_ART5_5
            || bestemmelse == USA_ART5_6
            || bestemmelse == USA_ART5_9;
    }
}
