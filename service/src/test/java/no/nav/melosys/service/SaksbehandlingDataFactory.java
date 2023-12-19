package no.nav.melosys.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public final class SaksbehandlingDataFactory {
    public static Behandling lagBehandling() {
        return lagBehandling(lagFagsak(), new MottatteOpplysningerData());
    }

    public static Behandling lagBehandling(MottatteOpplysningerData mottatteOpplysningerData) {
        return lagBehandling(lagFagsak(), mottatteOpplysningerData);
    }

    public static Behandling lagBehandling(Fagsak fagsak) {
        return lagBehandling(fagsak, new MottatteOpplysningerData());
    }

    public static Behandling lagBehandling(Fagsak fagsak, MottatteOpplysningerData mottatteOpplysningerData) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(fagsak);
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(mottatteOpplysningerData);
        final Instant nå = Instant.now();
        behandling.setRegistrertDato(nå.minus(30, ChronoUnit.DAYS));
        behandling.setEndretDato(nå);
        return behandling;
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
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.TRYGDETID);
        behandling.setFagsak(lagFagsak());
        final Instant nå = Instant.now();
        behandling.setRegistrertDato(nå.minus(30, ChronoUnit.DAYS));
        behandling.setEndretDato(nå);
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
        return lagFagsak("MEL-test");
    }

    public static Fagsak lagFagsak(String saksnummer) {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        fagsak.getAktører().add(lagBruker());
        fagsak.setGsakSaksnummer(123L);
        return fagsak;
    }

    public static Aktoer lagBruker() {
        var aktoer = new Aktoer();
        aktoer.setRolle(BRUKER);
        aktoer.setAktørId("aktørID");
        return aktoer;
    }
}
