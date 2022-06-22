package no.nav.melosys.service.sak;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.REGISTRERT_UNNTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FagsakServiceTest {

    @Mock
    private FagsakRepository fagsakRepo;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private FagsakService fagsakService;

    @BeforeEach
    public void setUp() {
        fagsakService = new FagsakService(fagsakRepo, behandlingService, kontaktopplysningService, oppgaveService, persondataFasade,
            behandlingsresultatService);
    }

    @Test
    void hentFagsak() {
        String saksnummer = "saksnummer";
        when(fagsakRepo.findBySaksnummer(anyString())).thenReturn(Optional.of(new Fagsak()));
        fagsakService.hentFagsak(saksnummer);
        verify(fagsakRepo).findBySaksnummer(saksnummer);
    }

    @Test
    void hentFagsakerMedAktør() {
        when(persondataFasade.hentAktørIdForIdent(any())).thenReturn("AKTOER_ID");
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "FNR");
        verify(fagsakRepo).findByRolleAndAktør(Aktoersroller.BRUKER, "AKTOER_ID");
    }

    @Test
    void lagre() {
        Fagsak fagsak = lagFagsak("12345");
        fagsakService.lagre(fagsak);
        verify(fagsakRepo).save(fagsak);
        assertThat(fagsak).isNotNull();
        assertThat(fagsak.getSaksnummer()).isNotEmpty();
    }

    @Test
    void nyFagsakOgBehandling() {
        Behandling behandling = mock(Behandling.class);
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";
        doReturn(behandling).when(behandlingService).nyBehandling(any(), any(), any(), any(), anyString(), anyString());

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID("123456789")
            .medSakstype(Sakstyper.EU_EOS)
            .medSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG)
            .medBehandlingstype(SOEKNAD)
            .medBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .medArbeidsgiver("arbeidsgiver")
            .medFullmektig(new Fullmektig("orgnr", Representerer.ARBEIDSGIVER))
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        verify(fagsakRepo).save(any(Fagsak.class));
        verify(behandlingService).nyBehandling(any(), eq(Behandlingsstatus.OPPRETTET), eq(SOEKNAD),
            eq(Behandlingstema.UTSENDT_ARBEIDSTAKER), eq(initierendeJournalpostId), eq(initierendeDokumentId));
        assertThat(fagsak.getBehandlinger()).isNotEmpty();
        assertThat(fagsak.getType()).isEqualTo(Sakstyper.EU_EOS);
        assertThat(fagsak.getTema()).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        Aktoer forventetFullmektig = new Aktoer();
        forventetFullmektig.setFagsak(fagsak);
        forventetFullmektig.setRolle(Aktoersroller.REPRESENTANT);
        forventetFullmektig.setOrgnr("orgnr");
        forventetFullmektig.setRepresenterer(Representerer.ARBEIDSGIVER);
        assertThat(fagsak.finnRepresentant(Representerer.ARBEIDSGIVER)).isPresent().get()
            .usingRecursiveComparison().isEqualTo(forventetFullmektig);
    }

    @Test
    void nyFagsakOgBehandling_kontaktPersonFinnes_KontaktOpplysningOpprettes() {
        Kontaktopplysning kontaktopplysning = Kontaktopplysning.av("RepresentantOrgnr", "Kontaktperson", "Telefon");
        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID("123456789")
            .medBehandlingstype(SOEKNAD)
            .medKontaktopplysninger(List.of(kontaktopplysning)).build();

        fagsakService.nyFagsakOgBehandling(opprettSakRequest);

        verify(kontaktopplysningService).lagEllerOppdaterKontaktopplysning(
            any(), eq("RepresentantOrgnr"), eq(null), eq("Kontaktperson"), eq("Telefon")
        );
    }

    @Test
    void avsluttFagsakOgBehandlingValiderBehandlingstema_behtemaIkkeYrkesaktiv_blirAvsluttet() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(SOEKNAD);
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.LOVVALG_AVKLART);
        verify(behandlingService).avsluttBehandling(behandling.getId());
    }

    @Test
    void avsluttFagsakOgBehandlingValiderBehandlingstype_behtemaTrygdetid_blirAvsluttet() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(SED);
        behandling.setTema(Behandlingstema.TRYGDETID);
        behandling.setStatus(UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.AVSLUTTET);
        verify(behandlingService).avsluttBehandling(behandling.getId());
    }

    @Test
    void avsluttFagsakOgBehandlingValiderBehandlingstype_behtemaUtsendtArbeidstaker_kasterException() {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling))
            .withMessageContaining("kan ikke avsluttes manuelt");
    }

    @Test
    void leggTilFjernAktørerForMyndighet() {
        String saksnummer = "1234";
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet(saksnummer);
        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(eksisterendeFagsak));

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheterForEuEos(saksnummer, nyeInstitusjonsIder);

        ArgumentCaptor<Fagsak> captor = ArgumentCaptor.forClass(Fagsak.class);
        verify(fagsakRepo).save(captor.capture());
        Fagsak oppdaterFagsak = captor.getValue();
        assertThat(oppdaterFagsak.getAktører().stream()
            .map(Aktoer::getInstitusjonId)
            .collect(Collectors.toList())).isSubsetOf(nyeInstitusjonsIder);
    }

    @Test
    void oppdaterMyndigheter_harBruker_fjernerIkkeBruker() {
        String saksnummer = "1234";
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet(saksnummer);
        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(eksisterendeFagsak));

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(eksisterendeFagsak);
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("1234");
        eksisterendeFagsak.getAktører().add(bruker);

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheterForEuEos(saksnummer, nyeInstitusjonsIder);

        ArgumentCaptor<Fagsak> captor = ArgumentCaptor.forClass(Fagsak.class);
        verify(fagsakRepo).save(captor.capture());
        Fagsak oppdaterFagsak = captor.getValue();

        assertThat(oppdaterFagsak.getAktører())
            .extracting(Aktoer::getRolle, Aktoer::getAktørId, Aktoer::getInstitusjonId)
            .containsExactlyInAnyOrder(
                tuple(Aktoersroller.BRUKER, "1234", null),
                tuple(Aktoersroller.TRYGDEMYNDIGHET, null, "Ny institusjonsid")
            );
    }

    @Test
    void avsluttFagsakOgBehandling_erAktiv_blirAvsluttet() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.LOVVALG_AVKLART);
        verify(fagsakRepo).save(fagsak);
        verify(behandlingService).avsluttBehandling(behandling.getId());
    }

    @Test
    void avsluttFagsakOgBehandling_behandlingTilhørerAnnenFagsak_kasterException() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-99");

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("MEL-0");

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART))
            .withMessageContaining("tilhører ikke fagsak");
    }

    @Test
    void opprettNyVurderingBehandling_behandlingstypeEndretPeriode_kastException() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(1L, ENDRET_PERIODE, AVSLUTTET, Instant.now());

        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(saksnummer))
            .withMessageContaining("Kan ikke revurdere en behandling av type " + Behandlingstyper.ENDRET_PERIODE.getBeskrivelse());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivIkkeArt16_kastException() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(1L, null, UNDER_BEHANDLING, null);

        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(new Behandlingsresultat());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(saksnummer))
            .withMessageContaining("Kan ikke revurdere en aktiv behandling");
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningIkkeSendt_kastException() {
        final String saksnummer = "MEL-1";

        var behandling = lagBehandling(1L, SOEKNAD, ANMODNING_UNNTAK_SENDT, null);
        Fagsak fagsak = lagFagsakMedBruker();
        fagsak.setBehandlinger(List.of(behandling));
        var behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode(behandling, false);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(saksnummer))
            .withMessageContaining("Kan ikke revurdere en aktiv behandling");
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningSendt_nyBehandlingOpprettet() {
        final String saksnummer = "MEL-1";

        var behandling = lagBehandling(1L, SOEKNAD, ANMODNING_UNNTAK_SENDT, null);
        Fagsak fagsak = lagFagsakMedBruker();
        fagsak.setBehandlinger(List.of(behandling));
        var behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode(behandling, true);

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);

        long replikertBehandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, behandling.getType());
        verify(behandlingService).avsluttBehandling(behandling.getId());
        assertThat(replikertBehandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAvTemaBeslutningLovvalgNorge_nyBehandlingOpprettet() {
        final String saksnummer = "MEL-1";

        var behandling = lagBehandling(1L, SED, AVSLUTTET, null);
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        Fagsak fagsak = lagFagsakMedBruker();
        fagsak.setBehandlinger(List.of(behandling));
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), lagVedtakMetadata(Instant.now()), null);

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);


        long replikertBehandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);


        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);
        assertThat(replikertBehandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerErAvsluttet_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteVedtak() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();
        var idag = Instant.now();
        var igår = idag.minus(1, ChronoUnit.DAYS);

        var behandlingSomBleFattetIgår = lagBehandling(1L, SOEKNAD, AVSLUTTET, igår);
        var behandlingsresultatBleFattetIgår = lagBehandlingsresultat(behandlingSomBleFattetIgår, igår, lagVedtakMetadata(igår), null);

        var behandlingSomBleFattetIdag = lagBehandling(2L, SOEKNAD, AVSLUTTET, idag);
        var behandlingsresultatBleFattetIdag = lagBehandlingsresultat(behandlingSomBleFattetIdag, idag, lagVedtakMetadata(idag), null);

        fagsak.setBehandlinger(List.of(behandlingSomBleFattetIgår, behandlingSomBleFattetIdag));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleFattetIgår.getId())).thenReturn(behandlingsresultatBleFattetIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleFattetIdag.getId())).thenReturn(behandlingsresultatBleFattetIdag);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleFattetIdag, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerErAvsluttetSisteHarIkkeVedtak_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteVedtak() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();
        var idag = Instant.now();
        var igår = idag.minus(1, ChronoUnit.DAYS);

        var behandlingSomBleFattetIgår = lagBehandling(1L, SOEKNAD, AVSLUTTET, igår);
        var behandlingsresultatFattetIgår = lagBehandlingsresultat(behandlingSomBleFattetIgår, igår, lagVedtakMetadata(igår), null);

        var behandlingSomBleFattetIdag = lagBehandling(2L, SOEKNAD, AVSLUTTET, idag);
        var behandlingsresultatFattetIdag = lagBehandlingsresultat(behandlingSomBleFattetIdag, idag, null, null);

        fagsak.setBehandlinger(List.of(behandlingSomBleFattetIgår, behandlingSomBleFattetIdag));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleFattetIgår.getId())).thenReturn(behandlingsresultatFattetIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatFattetIdag.getId())).thenReturn(behandlingsresultatFattetIdag);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleFattetIgår, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingHarIkkeVedtak_replikerUtenBehandlingsresultatFraSistOppdaterteBehandling() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(2L, SOEKNAD, AVSLUTTET, Instant.now());
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), null, null);

        fagsak.setBehandlinger(List.of(behandling));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingMedNyttBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultat.getId())).thenReturn(behandlingsresultat);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingMedNyttBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerAvTypeSed_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteUnntak() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();
        var idag = Instant.now();
        var igår = idag.minus(1, ChronoUnit.DAYS);

        var behandlingSomBleRegistrertIgår = lagBehandling(1L, SED, AVSLUTTET, igår);
        var behandlingsresultatRegistrertIgår = lagBehandlingsresultat(behandlingSomBleRegistrertIgår, igår, null, REGISTRERT_UNNTAK);

        var behandlingSomBleRegistrertIdag = lagBehandling(2L, SED, AVSLUTTET, idag);
        var behandlingsresultatRegistrertIdag = lagBehandlingsresultat(behandlingSomBleRegistrertIdag, idag, null, REGISTRERT_UNNTAK);

        fagsak.setBehandlinger(List.of(behandlingSomBleRegistrertIgår, behandlingSomBleRegistrertIdag));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIgår.getId())).thenReturn(behandlingsresultatRegistrertIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIdag.getId())).thenReturn(behandlingsresultatRegistrertIdag);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleRegistrertIdag, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerAvTypeSedSisteIkkeRegistrertUnntak_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteUnntak() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();
        var idag = Instant.now();
        var igår = idag.minus(1, ChronoUnit.DAYS);


        var behandlingSomBleRegistrertIgår = lagBehandling(1L, SED, AVSLUTTET, igår);
        var behandlingsresultatRegistrertIgår = lagBehandlingsresultat(behandlingSomBleRegistrertIgår, igår, null, REGISTRERT_UNNTAK);

        var behandlingSomBleRegistrertIdag = lagBehandling(2L, SED, AVSLUTTET, idag);
        var behandlingsresultatRegistrertIdag = lagBehandlingsresultat(behandlingSomBleRegistrertIdag, idag, null, null);

        fagsak.setBehandlinger(List.of(behandlingSomBleRegistrertIgår, behandlingSomBleRegistrertIdag));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIgår.getId())).thenReturn(behandlingsresultatRegistrertIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIdag.getId())).thenReturn(behandlingsresultatRegistrertIdag);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleRegistrertIgår, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingerAvTypeSedHarIkkeRegistrertUnntakt_replikerUtenBehandlingsresultatFraSistOppdaterteBehandling() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(2L, SED, AVSLUTTET, Instant.now());
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), null, null);

        fagsak.setBehandlinger(List.of(behandling));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingMedNyttBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingMedNyttBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingerAvTypeAnnetEnnSedOgSøknad_replikerUtenBehandlingsresultatFraSistOppdaterteBehandling() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(2L, KLAGE, AVSLUTTET, Instant.now());
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), null, null);

        fagsak.setBehandlinger(List.of(behandling));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(saksnummer)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingMedNyttBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingMedNyttBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    private Fagsak lagFagsakMedAktørforMyndighet(String saksnummer) {
        Fagsak fagsak = lagFagsak(saksnummer);

        Aktoer aktoer = new Aktoer();
        aktoer.setInstitusjonId("Gammel institusjonsid");
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        fagsak.setAktører(new HashSet<>(Collections.singleton(aktoer)));
        return fagsak;
    }

    private Fagsak lagFagsakMedBruker() {
        Fagsak fagsak = lagFagsak("MEL-1");

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId("12312");
        aktoer.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Set.of(aktoer));
        return fagsak;
    }

    private Fagsak lagFagsak(String saksnummer) {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setSaksnummer(saksnummer);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setRegistrertDato(Instant.now());
        fagsak.setEndretDato(Instant.now());
        return fagsak;
    }

    private Behandling lagBehandling(long id, Behandlingstyper type, Behandlingsstatus status, Instant registrertDato) {
        var behandling = new Behandling();
        behandling.setId(id);
        behandling.setType(type);
        behandling.setStatus(status);
        behandling.setEndretDato(registrertDato);
        behandling.setRegistrertDato(registrertDato);
        return behandling;
    }

    private Behandlingsresultat lagBehandlingsresultat(Behandling behandling, Instant registrertDato, VedtakMetadata vedtakMetadata, Behandlingsresultattyper type) {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(behandling.getId());
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setRegistrertDato(registrertDato);
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        behandlingsresultat.setType(type);
        return behandlingsresultat;
    }

    private Behandlingsresultat lagBehandlingsresultatMedAnmodningsperiode(Behandling behandling, boolean sendtTilUtlandet) {
        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(sendtTilUtlandet);
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        behandlingsresultat.setBehandling(behandling);
        return behandlingsresultat;
    }

    private VedtakMetadata lagVedtakMetadata(Instant registrertDato) {
        var vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setRegistrertDato(registrertDato);
        return vedtakMetadata;
    }
}
