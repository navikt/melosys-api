package no.nav.melosys.service.kontroll.ferdigbehandling;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.service.kontroll.arbeidutland.ArbeidUtlandKontroller;
import no.nav.melosys.service.kontroll.regler.ArbeidsstedRegler;
import no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.kontroll.regler.PersonRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

final class FerdigbehandlingKontroller {


    static Kontrollfeil overlappendeMedlemsperiode(FerdigbehandlingKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();
        Lovvalgsperiode opprinneligLovvalgsperiode = kontrollData.getOpprinneligLovvalgsperiode();

        return OverlappendeMedlemskapsperioderRegler.harOverlappendeMedlemsperiode(medlemskapDokument,
            lovvalgsperiode, opprinneligLovvalgsperiode) ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER) : null;
    }

    static Kontrollfeil periodeOver24Mnd(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.erArtikkel12()
            && PeriodeRegler.periodeOver24Mnd(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) : null;
    }

    static Kontrollfeil periodeManglerSluttdato(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getTom() == null ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil periodeOverTreÅr(FerdigbehandlingKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1
            && PeriodeRegler.periodeOver3År(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_TRE_ÅR) : null;
    }

    static Kontrollfeil arbeidsstedManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroller.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil foretakUtlandManglerFelter(FerdigbehandlingKontrollData kontrollData) {
        return ArbeidUtlandKontroller.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil representantIUtlandetMangler(FerdigbehandlingKontrollData kontrollData) {
        var lovvalgsperiode = kontrollData.getLovvalgsperiode();
        var behandlingsgrunnlagData = (SoeknadTrygdeavtale) kontrollData.getBehandlingsgrunnlagData();

        return erBestemmelseDerTrygdeavtaleAttestSendes(lovvalgsperiode.getBestemmelse())
            && ArbeidsstedRegler.representantIUtlandetMangler(behandlingsgrunnlagData.getRepresentantIUtlandet())
            ? new Kontrollfeil(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED) : null;
    }

    static Kontrollfeil adresseRegistrert(FerdigbehandlingKontrollData kontrollData) {
        return PersonRegler.harRegistrertAdresse(kontrollData.getPersondata(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    private static boolean erBestemmelseDerTrygdeavtaleAttestSendes(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1
            || bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_5
            || bestemmelse == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART7_3;
    }
}
