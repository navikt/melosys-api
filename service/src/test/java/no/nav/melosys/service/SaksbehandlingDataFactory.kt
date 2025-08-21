package no.nav.melosys.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class SaksbehandlingDataFactory {
    public static Behandling lagBehandling() {
        return lagBehandling(lagFagsak(), new MottatteOpplysningerData());
    }

    public static Behandling lagBehandlingMedMedlemskapDokument() {
        return lagBehandlingMedMedlemskapDokument(lagFagsak(), new MottatteOpplysningerData());
    }

    public static Behandling lagBehandling(MottatteOpplysningerData mottatteOpplysningerData) {
        return lagBehandling(lagFagsak(), mottatteOpplysningerData);
    }

    public static Behandling lagBehandling(Fagsak fagsak) {
        return lagBehandling(fagsak, new MottatteOpplysningerData());
    }

    public static Behandling lagBehandling(Fagsak fagsak, MottatteOpplysningerData mottatteOpplysningerData) {
        final Instant nå = Instant.now();
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medFagsak(fagsak)
            .medMottatteOpplysninger(new MottatteOpplysninger())
            .medRegistrertDato(nå.minus(30, ChronoUnit.DAYS))
            .medEndretDato(nå)
            .build();
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(mottatteOpplysningerData);
        return behandling;
    }

    public static Behandling lagBehandlingMedMedlemskapDokument(Fagsak fagsak, MottatteOpplysningerData mottatteOpplysningerData) {
        Behandling behandling = lagBehandling(fagsak, mottatteOpplysningerData);
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.kode);
        MedlemskapDokument medlDokument = new MedlemskapDokument();
        Saksopplysning medl = new Saksopplysning();

        medlDokument.medlemsperiode.add(medlemsperiode);
        medl.setDokument(medlDokument);
        medl.setType(SaksopplysningType.MEDL);

        behandling.setSaksopplysninger(Set.of(medl));

        return behandling;
    }

    private static Medlemsperiode lagMedlemsperiode(long id, String grunnlagMedlKode) {
        Periode periode = new Periode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        return new Medlemsperiode(
            id, periode, null,
            PeriodestatusMedl.GYLD.kode, grunnlagMedlKode, null, null, null, null, null);
    }
    public static Behandling lagInaktivBehandling() {
        var behandling = lagBehandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        return behandling;
    }

    public static Behandling lagInaktivBehandling(Fagsak fagsak) {
        var behandling = lagBehandling();
        behandling.setFagsak(fagsak);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        return behandling;
    }

    public static Behandling lagInaktivBehandlingSomIkkeResulterIVedtak() {
        final Instant nå = Instant.now();
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.AVSLUTTET)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.TRYGDETID)
            .medFagsak(lagFagsak())
            .medRegistrertDato(nå.minus(30, ChronoUnit.DAYS))
            .medEndretDato(nå)
            .build();
        return behandling;
    }

    public static Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        final var vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        vedtakMetadata.setVedtaksdato(Instant.now().minus(3, ChronoUnit.DAYS));
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        return behandlingsresultat;
    }

    public static Fagsak lagFagsak() {
        return FagsakTestFactory.builder().medBruker().medGsakSaksnummer().build();
    }
}
