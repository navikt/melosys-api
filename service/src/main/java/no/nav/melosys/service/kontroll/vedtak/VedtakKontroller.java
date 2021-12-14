package no.nav.melosys.service.kontroll.vedtak;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.service.kontroll.*;
import no.nav.melosys.service.validering.Kontrollfeil;

final class VedtakKontroller implements AdresseUtlandKontroller {

    private VedtakKontroller() {
    }

    static Kontrollfeil overlappendeMedlemsperiode(VedtakKontrollData kontrollData) {
        MedlemskapDokument medlemskapDokument = kontrollData.getMedlemskapDokument();
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return MedlemskapKontroller.overlappendeMedlemsperiodeGyldigPeriode(lovvalgsperiode.getFom(), lovvalgsperiode.getTom(), medlemskapDokument)
            ? new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER) : null;
    }

    static Kontrollfeil periodeOver24Mnd(VedtakKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.erArtikkel12()
            && PeriodeKontroller.periodeOver24Mnd(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) : null;
    }

    static Kontrollfeil periodeManglerSluttdato(VedtakKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getTom() == null ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil periodeOverTreÅr(VedtakKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getBestemmelse() == Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1
            && PeriodeKontroller.periodeOver3År(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.MER_ENN_TRE_ÅR) : null;
    }

    static Kontrollfeil arbeidsstedManglerFelter(VedtakKontrollData kontrollData) {
        return AdresseUtlandKontroller.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil foretakUtlandManglerFelter(VedtakKontrollData kontrollData) {
        return AdresseUtlandKontroller.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil representantIUtlandetMangler(VedtakKontrollData kontrollData) {
        var lovvalgsperiode = kontrollData.getLovvalgsperiode();
        var behandlingsgrunnlagData = (SoeknadTrygdeavtale) kontrollData.getBehandlingsgrunnlagData();

        return lovvalgsperiode.skalFåTrygdeavtaleAttest()
            && ArbeidsstedKontroller.representantIUtlandetMangler(behandlingsgrunnlagData.getRepresentantIUtlandet())
            ? new Kontrollfeil(Kontroll_begrunnelser.ATTEST_MANGER_ARBEIDSSTED) : null;
    }

    static Kontrollfeil adresseRegistrert(VedtakKontrollData kontrollData) {
        return PersonKontroller.harRegistrertAdresse(kontrollData.getPersonDokument(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }
}
