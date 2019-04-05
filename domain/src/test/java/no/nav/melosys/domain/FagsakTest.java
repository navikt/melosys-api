package no.nav.melosys.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.TekniskException;
import org.assertj.core.util.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FagsakTest {

    @Test
    public void getAktivBehandling() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(Behandlingsstatus.AVSLUTTET);

        Behandling b2 = new Behandling();
        b2.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        Behandling b3 = new Behandling();
        b3.setStatus(Behandlingsstatus.AVSLUTTET);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        behandlinger.add(b3);
        fagsak.setBehandlinger(behandlinger);

        Behandling aktivBehandling = fagsak.getAktivBehandling();

        assertThat(aktivBehandling).isEqualTo(b2);
    }

    @Test
    public void getTidligsteInaktivBehandling_toInaktive() {
        Fagsak fagsak = new Fagsak();
        Behandling tidligsteInaktiveBehandling = new Behandling();
        tidligsteInaktiveBehandling.setRegistrertDato(Instant.parse("2019-01-10T10:37:30.00Z"));
        tidligsteInaktiveBehandling.setStatus(Behandlingsstatus.AVSLUTTET);

        Behandling aktivBehandling = new Behandling();
        aktivBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        Behandling seinesteInaktiveBehandling = new Behandling();
        seinesteInaktiveBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        seinesteInaktiveBehandling.setRegistrertDato(Instant.parse("2019-02-10T10:37:30.00Z"));

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(tidligsteInaktiveBehandling);
        behandlinger.add(aktivBehandling);
        behandlinger.add(seinesteInaktiveBehandling);
        fagsak.setBehandlinger(behandlinger);

        assertThat(fagsak.getTidligsteInaktiveBehandling()).isEqualTo(tidligsteInaktiveBehandling);
    }

    @Test
    public void getAktivBehandling_ingenAktive() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(Behandlingsstatus.AVSLUTTET);

        Behandling b2 = new Behandling();
        b2.setStatus(Behandlingsstatus.AVSLUTTET);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        fagsak.setBehandlinger(behandlinger);

        Behandling aktivBehandling = fagsak.getAktivBehandling();

        assertThat(aktivBehandling).isNull();
    }

    @Test(expected = TekniskException.class)
    public void getAktivBehandling_feilTilstand() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(Behandlingsstatus.AVVENT_DOK_PART);

        Behandling b2 = new Behandling();
        b2.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        fagsak.setBehandlinger(behandlinger);

        fagsak.getAktivBehandling();
    }

    @Test
    public void getBruker() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.BRUKER);
        a1.setAktørId("123");
        fagsak.getAktører().add(a1);
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.ARBEIDSGIVER);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Aktoer bruker = fagsak.hentAktørMedRolleType(Aktoersroller.BRUKER);

        assertThat(bruker).isEqualTo(a1);
    }

    @Test
    public void getBruker_ingen() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.ARBEIDSGIVER);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Aktoer bruker = fagsak.hentAktørMedRolleType(Aktoersroller.BRUKER);

        assertThat(bruker).isNull();
    }

    @Test(expected = TekniskException.class)
    public void getBruker_flere() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.BRUKER);
        a1.setAktørId("123");
        fagsak.getAktører().add(a1);
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.BRUKER);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Aktoer bruker = fagsak.hentAktørMedRolleType(Aktoersroller.BRUKER);

        assertThat(bruker).isEqualTo(a1);
    }

    @Test
    public void hentMyndighetLandkode_forventGyldigLandkode() throws Exception {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.MYNDIGHET);
        aktoer.setInstitusjonId("SE:gfr");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Sets.newLinkedHashSet(aktoer));

        Landkoder resultat = fagsak.hentMyndighetLandkode();
        assertThat(resultat).isEqualByComparingTo(Landkoder.SE);
    }

    @Test(expected = TekniskException.class)
    public void hentMyndighetLandkode_aktoerIkkeMyndighet_forventTekniskException() throws Exception {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setInstitusjonId("SE:gfr");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Sets.newLinkedHashSet(aktoer));

        fagsak.hentMyndighetLandkode();
    }

    @Test
    public void harAktørMedRolleTypeArbeidsgiver_arbeidsgiverFinnes_forventTrue() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.ARBEIDSGIVER);

        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(aktoer);

        assertThat(fagsak.harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER)).isTrue();
    }

    @Test
    public void harAktørMedRolleTypeArbeidsgiver_kunBruker_forventFalse() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);

        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(aktoer);

        assertThat(fagsak.harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER)).isFalse();
    }
}