package no.nav.melosys.domain;

import java.time.Instant;
import java.util.*;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FagsakTest {

    @Test
    void getAktivBehandling() throws TekniskException {
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

        Behandling aktivBehandling = fagsak.hentAktivBehandling();

        assertThat(aktivBehandling).isEqualTo(b2);
    }

    @Test
    void getTidligsteInaktivBehandling_toInaktive() {
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
    void getSistOppdaterteBehandling_medEnBehandling() throws FunksjonellException {
        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setEndretDato(Instant.parse("2019-01-10T10:37:30.00Z"));
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        assertThat(fagsak.getSistOppdaterteBehandling()).isEqualTo(behandling);
    }

    @Test
    void getSistOppdaterteBehandling_medTreBehandlinger() throws FunksjonellException {
        Fagsak fagsak = new Fagsak();

        Behandling sistOppdaterteBehandling = new Behandling();
        sistOppdaterteBehandling.setEndretDato(Instant.parse("2019-01-10T10:37:30.00Z"));

        Behandling behandling1 = new Behandling();
        behandling1.setEndretDato(Instant.parse("2019-01-10T10:36:30.00Z"));

        Behandling behandling2 = new Behandling();
        behandling2.setEndretDato(Instant.parse("2019-01-09T10:37:30.00Z"));

        fagsak.setBehandlinger(Arrays.asList(
            sistOppdaterteBehandling,
            behandling1,
            behandling2
        ));

        assertThat(fagsak.getSistOppdaterteBehandling()).isEqualTo(sistOppdaterteBehandling);
    }

    @Test
    void getSistOppdaterteBehandling_ingenBehandlinger_kasterException() throws FunksjonellException {
        var fagsak = new Fagsak();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(fagsak::getSistOppdaterteBehandling)
            .withMessageContaining("Finner ikke behandlinger");
    }

    @Test
    void getAktivBehandling_ingenAktive() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(Behandlingsstatus.AVSLUTTET);

        Behandling b2 = new Behandling();
        b2.setStatus(Behandlingsstatus.AVSLUTTET);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        fagsak.setBehandlinger(behandlinger);

        Behandling aktivBehandling = fagsak.hentAktivBehandling();

        assertThat(aktivBehandling).isNull();
    }

    @Test
    void getAktivBehandling_feilTilstand() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(Behandlingsstatus.AVVENT_DOK_PART);

        Behandling b2 = new Behandling();
        b2.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        fagsak.setBehandlinger(behandlinger);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(fagsak::hentAktivBehandling)
            .withMessageContaining("mer enn en");
    }

    @Test
    void getBruker() throws TekniskException {
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

        Aktoer bruker = fagsak.hentBruker();

        assertThat(bruker).isEqualTo(a1);
    }

    @Test
    void getBruker_ingen() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.ARBEIDSGIVER);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Aktoer bruker = fagsak.hentBruker();

        assertThat(bruker).isNull();
    }

    @Test
    void getBruker_flere() throws TekniskException {
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

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(fagsak::hentBruker)
            .withMessageContaining("mer enn en");
    }

    @Test
    void hentRepresentant_arbeidsgiver_funker() {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.REPRESENTANT);
        a1.setRepresenterer(Representerer.BRUKER);
        a1.setAktørId("123");
        fagsak.getAktører().add(a1);
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.REPRESENTANT);
        a2.setRepresenterer(Representerer.ARBEIDSGIVER);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.ARBEIDSGIVER);

        assertThat(representant).isEqualTo(Optional.of(a2));
    }

    @Test
    void hentRepresentant_begge_funker() {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.REPRESENTANT);
        a1.setRepresenterer(Representerer.ARBEIDSGIVER);
        a1.setAktørId("123");
        fagsak.getAktører().add(a1);
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.REPRESENTANT);
        a2.setRepresenterer(Representerer.BEGGE);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);

        assertThat(representant).isEqualTo(Optional.of(a2));
    }

    @Test
    void hentMyndighetLandkode_forventGyldigLandkode() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.MYNDIGHET);
        aktoer.setInstitusjonId("SE:gfr");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Sets.newLinkedHashSet(aktoer));

        Landkoder resultat = fagsak.hentMyndighetLandkode();
        assertThat(resultat).isEqualByComparingTo(Landkoder.SE);
    }

    @Test
    void hentMyndighetLandkode_aktoerIkkeMyndighet_forventTekniskException() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setInstitusjonId("SE:gfr");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Sets.newLinkedHashSet(aktoer));

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(fagsak::hentMyndighetLandkode)
            .withMessageContaining("Finner ingen aktør");
    }

    @Test
    void harAktørMedRolleTypeArbeidsgiver_arbeidsgiverFinnes_forventTrue() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.ARBEIDSGIVER);

        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(aktoer);

        assertThat(fagsak.harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER)).isTrue();
    }

    @Test
    void harAktørMedRolleTypeArbeidsgiver_kunBruker_forventFalse() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);

        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(aktoer);

        assertThat(fagsak.harAktørMedRolleType(Aktoersroller.ARBEIDSGIVER)).isFalse();
    }
}
