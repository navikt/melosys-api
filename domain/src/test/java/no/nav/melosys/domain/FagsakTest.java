package no.nav.melosys.domain;

import java.time.Instant;
import java.util.*;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FagsakTest {

    @Test
    void getAktivBehandling() {
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
    void hentTidligsteInaktivBehandling_toInaktive() {
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

        assertThat(fagsak.hentTidligstInaktivBehandling()).isEqualTo(tidligsteInaktiveBehandling);
    }

    @Test
    void getSistOppdaterteBehandling_medEnBehandling() {
        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setEndretDato(Instant.parse("2019-01-10T10:37:30.00Z"));
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        assertThat(fagsak.hentSistOppdatertBehandling()).isEqualTo(behandling);
    }

    @Test
    void getSistOppdaterteBehandling_medTreBehandlinger() {
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

        assertThat(fagsak.hentSistOppdatertBehandling()).isEqualTo(sistOppdaterteBehandling);
    }

    @Test
    void hentBehandlingerSortertPåRegistertDato_medToBehandlinger_sortertRiktig() {
        Fagsak fagsak = new Fagsak();

        Behandling behandling1 = new Behandling();
        behandling1.setRegistrertDato(Instant.parse("2020-01-01T00:00:00Z"));

        Behandling behandling2 = new Behandling();
        behandling2.setRegistrertDato(Instant.parse("2021-01-01T00:00:00Z"));

        fagsak.setBehandlinger(List.of(behandling1, behandling2));

        List<Instant> registrerteDatoer = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato().stream().map(Behandling::getRegistrertDato).toList();
        assertThat(registrerteDatoer)
            .isEqualTo(List.of(behandling2.registrertDato, behandling1.registrertDato));
    }

    @Test
    void hentSistOppdatertBehandling_medToBehandlinger_returnerNyeste() {
        Fagsak fagsak = new Fagsak();

        Behandling behandling1 = new Behandling();
        behandling1.setRegistrertDato(Instant.parse("2020-01-01T00:00:00Z"));

        Behandling behandling2 = new Behandling();
        behandling2.setRegistrertDato(Instant.parse("2021-01-01T00:00:00Z"));

        fagsak.setBehandlinger(List.of(behandling1, behandling2));

        assertThat(fagsak.hentSistRegistrertBehandling().getRegistrertDato())
            .isEqualTo(behandling2.getRegistrertDato());
    }

    @Test
    void getSistOppdaterteBehandling_ingenBehandlinger_kasterException() {
        var fagsak = new Fagsak();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(fagsak::hentSistOppdatertBehandling)
            .withMessageContaining("Finner ikke behandlinger");
    }

    @Test
    void getAktivBehandling_ingenAktive() {
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
    void getAktivBehandling_feilTilstand() {
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
    void getBruker() {
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
    void getBruker_ingen() {
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
    void getBruker_flere() {
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
    void finnFullmektig_arbeidsgiver_funker() {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.FULLMEKTIG);
        a1.setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD);
        a1.setAktørId("123");
        fagsak.getAktører().add(a1);
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.FULLMEKTIG);
        a2.setFullmaktstype(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Optional<Aktoer> fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);

        assertThat(fullmektig).isEqualTo(Optional.of(a2));
    }

    @Test
    void finnFullmektig_bruker_funker() {
        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(new HashSet<>());
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.FULLMEKTIG);
        a1.setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD);
        a1.setAktørId("123");
        fagsak.getAktører().add(a1);
        Aktoer a2 = new Aktoer();
        a2.setRolle(Aktoersroller.FULLMEKTIG);
        a2.setFullmaktstype(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
        a2.setAktørId("456");
        fagsak.getAktører().add(a2);

        Optional<Aktoer> fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD);

        assertThat(fullmektig).isEqualTo(Optional.of(a1));
    }

    @Test
    void hentMyndighetLandkode_forventGyldigLandkode() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        aktoer.setInstitusjonID("SE:gfr");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Sets.newLinkedHashSet(aktoer));

        Land_iso2 resultat = fagsak.hentMyndighetLandkode();
        assertThat(resultat).isEqualByComparingTo(Land_iso2.SE);
    }

    @Test
    void hentMyndighetLandkode_aktoerIkkeMyndighet_forventTekniskException() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setInstitusjonID("SE:gfr");

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
