package no.nav.melosys.service.kontroll.vedtak;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
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

        return (lovvalgsperiode.erArtikkel12() || lovvalgsperiode.skalFåTrygdeavtaleAttest())
            && PeriodeKontroller.periodeOver24Mnd(lovvalgsperiode.getFom(), lovvalgsperiode.getTom())
            ? new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) : null;
    }

    static Kontrollfeil periodeManglerSluttdato(VedtakKontrollData kontrollData) {
        Lovvalgsperiode lovvalgsperiode = kontrollData.getLovvalgsperiode();

        return lovvalgsperiode.getTom() == null ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil arbeidsstedManglerFelter(VedtakKontrollData kontrollData) {
        return AdresseUtlandKontroller.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil foretakUtlandManglerFelter(VedtakKontrollData kontrollData) {
        return AdresseUtlandKontroller.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData());
    }

    static Kontrollfeil representantIUtlandetManglerFelter(VedtakKontrollData kontrollData) {
        var lovvalgsperiode = kontrollData.getLovvalgsperiode();
        var behandlingsgrunnlagData = (SoeknadTrygdeavtale) kontrollData.getBehandlingsgrunnlagData();

        return lovvalgsperiode.skalFåTrygdeavtaleAttest()
            && ArbeidsstedKontroller.representantIUtlandetManglerFelter(behandlingsgrunnlagData.getRepresentantIUtlandet())
            ? new Kontrollfeil(Kontroll_begrunnelser.ANNET) : null; // TODO: Endre til nytt kodeverk-objekt når det kommer.
    }

    static Kontrollfeil adresseRegistrertForA1(VedtakKontrollData kontrollData) {
        return PersonKontroller.harRegistrertAdresse(kontrollData.getPersonDokument(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }
}
