package no.nav.melosys.service.sak;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_ARBEIDSTAKER;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FagsakServiceTest {
    private final static String SAKSNUMMER = "MEL-123";

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
        fagsakService = new FagsakService(
            fagsakRepo,
            behandlingService,
            kontaktopplysningService,
            oppgaveService,
            persondataFasade,
            behandlingsresultatService);
    }

    @Test
    void hentFagsak() {
        when(fagsakRepo.findBySaksnummer(anyString())).thenReturn(Optional.of(new Fagsak()));
        fagsakService.hentFagsak(SAKSNUMMER);
        verify(fagsakRepo).findBySaksnummer(SAKSNUMMER);
    }

    @Test
    void hentFagsakerMedAktør() {
        when(persondataFasade.hentAktørIdForIdent(any())).thenReturn("AKTOER_ID");
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "FNR");
        verify(fagsakRepo).findByRolleAndAktør(Aktoersroller.BRUKER, "AKTOER_ID");
    }

    @Test
    void lagre() {
        Fagsak fagsak = lagFagsak();
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
        doReturn(behandling).when(behandlingService).nyBehandling(any(), any(), any(), any(), anyString(), anyString(), any(), any(), anyString());

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID("123456789")
            .medSakstype(EU_EOS)
            .medSakstema(MEDLEMSKAP_LOVVALG)
            .medBehandlingstype(SOEKNAD)
            .medBehandlingstema(UTSENDT_ARBEIDSTAKER)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .medArbeidsgiver("arbeidsgiver")
            .medFullmektig(new Fullmektig("orgnr", Representerer.ARBEIDSGIVER))
            .medBehandlingsårsaktype(Behandlingsaarsaktyper.FRITEKST)
            .medBehandlingsårsakFritekst("Fritekst")
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        verify(fagsakRepo).save(any(Fagsak.class));
        verify(behandlingService).nyBehandling(any(), eq(Behandlingsstatus.OPPRETTET), eq(SOEKNAD),
            eq(UTSENDT_ARBEIDSTAKER), eq(initierendeJournalpostId), eq(initierendeDokumentId), any(),
            eq(Behandlingsaarsaktyper.FRITEKST), eq("Fritekst"));
        assertThat(fagsak.getBehandlinger()).isNotEmpty();
        assertThat(fagsak.getType()).isEqualTo(EU_EOS);
        assertThat(fagsak.getTema()).isEqualTo(MEDLEMSKAP_LOVVALG);
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
    void hentMuligeSakstema_med_behandlingstema_med_behandlingstema_lovlig() {
        Fagsak fagsak = lagFagsakMedBruker();
        Behandling behandling = lagBehandling(1L, SOEKNAD, UNDER_BEHANDLING, Instant.now());
        behandling.setTema(UTSENDT_ARBEIDSTAKER);
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setTema(MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        Set<Sakstemaer> muligeSakstemaer = fagsakService.hentMuligeSakstemaer(SAKSNUMMER);

        assertThat(muligeSakstemaer)
            .isNotEmpty()
            .contains(UNNTAK, TRYGDEAVGIFT)
            .doesNotContain(MEDLEMSKAP_LOVVALG);
    }

    @Test
    @Disabled("Frem til sakstype fiks")
    void hentMuligeSakstyper_med_behandlingstema_med_behandlingstema_lovlig() {
        Fagsak fagsak = lagFagsakMedBruker();
        Behandling behandling = lagBehandling(1L, SOEKNAD, UNDER_BEHANDLING, Instant.now());
        behandling.setTema(UTSENDT_ARBEIDSTAKER);
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setType(EU_EOS);
        behandling.setFagsak(fagsak);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        Set<Sakstyper> muligeSakstyper = fagsakService.hentMuligeSakstyper(SAKSNUMMER);

        assertThat(muligeSakstyper)
            .isNotEmpty()
            .contains(TRYGDEAVTALE, FTRL)
            .doesNotContain(EU_EOS);
    }

    @Test
    @Disabled("Frem til sakstype fiks")
    void endreFagsakTypeMedMuligeVerdier_med_behandlingstema_lovlig() {
        Fagsak fagsak = lagFagsakMedBruker();
        Behandling behandling = lagBehandling(1L, SOEKNAD, UNDER_BEHANDLING, Instant.now());
        behandling.setTema(UTSENDT_ARBEIDSTAKER);
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setType(EU_EOS);
        fagsak.setTema(MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        Set<Sakstyper> muligeSakstyper = fagsakService.hentMuligeSakstyper(SAKSNUMMER);
        Optional<Sakstyper> valgtSakstype = muligeSakstyper.stream().findFirst();

        assertThat(valgtSakstype).isNotNull();

        fagsakService.endreSakstype(fagsak, valgtSakstype.get());

        assertThat(fagsak.getType()).isEqualTo(valgtSakstype.get());
    }

    @Test
    void endreFagsakTemaMedMuligeVerdier_med_behandlingstema_lovlig() {
        Fagsak fagsak = lagFagsakMedBruker();
        Behandling behandling = lagBehandling(1L, SOEKNAD, UNDER_BEHANDLING, Instant.now());
        behandling.setTema(UTSENDT_ARBEIDSTAKER);
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setType(EU_EOS);
        fagsak.setTema(MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        Set<Sakstemaer> muligeSakstemaer = fagsakService.hentMuligeSakstemaer(SAKSNUMMER);
        Optional<Sakstemaer> valgtSakstema = muligeSakstemaer.stream().findFirst();

        assertThat(valgtSakstema).isNotNull();

        fagsakService.endreSakstema(fagsak, valgtSakstema.get());

        assertThat(fagsak.getTema()).isEqualTo(valgtSakstema.get());
    }

    @Test
    void hentFagsakTypeOgTemaMedMuligeVerdier_med_behandlingstema_ikke_lovlig() {
        Fagsak fagsak = lagFagsakMedBruker();
        Behandling behandling = lagBehandling(1L, SOEKNAD, UNDER_BEHANDLING, Instant.now());
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        fagsak.setBehandlinger(List.of(behandling));
        fagsak.setType(EU_EOS);
        fagsak.setTema(MEDLEMSKAP_LOVVALG);
        behandling.setFagsak(fagsak);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        Set<Sakstyper> muligeSakstyper = fagsakService.hentMuligeSakstyper(SAKSNUMMER);
        Set<Sakstemaer> muligeSakstemaer = fagsakService.hentMuligeSakstemaer(SAKSNUMMER);

        Optional<Sakstemaer> valgtSakstema = muligeSakstemaer.stream().findFirst();
        Optional<Sakstyper> valgtSakstype = muligeSakstyper.stream().findFirst();

        assertThat(valgtSakstype).isEmpty();
        assertThat(valgtSakstema).isEmpty();
    }

    @Test
    void leggTilFjernAktørerForMyndighet() {
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet();
        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(eksisterendeFagsak));

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheterForEuEos(SAKSNUMMER, nyeInstitusjonsIder);

        ArgumentCaptor<Fagsak> captor = ArgumentCaptor.forClass(Fagsak.class);
        verify(fagsakRepo).save(captor.capture());
        Fagsak oppdaterFagsak = captor.getValue();
        assertThat(oppdaterFagsak.getAktører().stream()
            .map(Aktoer::getInstitusjonId)
            .collect(Collectors.toList())).isSubsetOf(nyeInstitusjonsIder);
    }

    @Test
    void oppdaterMyndigheter_harBruker_fjernerIkkeBruker() {
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet();
        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(eksisterendeFagsak));

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(eksisterendeFagsak);
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("1234");
        eksisterendeFagsak.getAktører().add(bruker);

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheterForEuEos(SAKSNUMMER, nyeInstitusjonsIder);

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
        fagsak.setSaksnummer(SAKSNUMMER);
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
    void avsluttFagsakOgBehandling_manglerBehandling_avslutterFagsak() {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.AVSLUTTET);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.AVSLUTTET);
        verify(fagsakRepo).save(fagsak);
        verify(behandlingService, never()).avsluttBehandling(anyLong());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingstypeEndretPeriode_kastException() {
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(1L, ENDRET_PERIODE, AVSLUTTET, Instant.now());

        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(SAKSNUMMER))
            .withMessageContaining("Kan ikke revurdere en behandling av type " + Behandlingstyper.ENDRET_PERIODE.getBeskrivelse());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivIkkeArt16_kastException() {
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(1L, null, UNDER_BEHANDLING, null);

        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(new Behandlingsresultat());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(SAKSNUMMER))
            .withMessageContaining("Kan ikke revurdere en aktiv behandling");
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningIkkeSendt_kastException() {
        var behandling = lagBehandling(1L, SOEKNAD, ANMODNING_UNNTAK_SENDT, null);
        Fagsak fagsak = lagFagsakMedBruker();
        fagsak.setBehandlinger(List.of(behandling));
        var behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode(behandling, false);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(SAKSNUMMER))
            .withMessageContaining("Kan ikke revurdere en aktiv behandling");
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningSendt_nyBehandlingOpprettet() {
        var behandling = lagBehandling(1L, SOEKNAD, ANMODNING_UNNTAK_SENDT, null);
        Fagsak fagsak = lagFagsakMedBruker();
        fagsak.setBehandlinger(List.of(behandling));
        var behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode(behandling, true);

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);

        long replikertBehandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, behandling.getType());
        verify(behandlingService).avsluttBehandling(behandling.getId());
        assertThat(replikertBehandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAvTemaBeslutningLovvalgNorge_nyBehandlingOpprettet() {
        var behandling = lagBehandling(1L, SED, AVSLUTTET, null);
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        Fagsak fagsak = lagFagsakMedBruker();
        fagsak.setBehandlinger(List.of(behandling));
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), lagVedtakMetadata(Instant.now()), null);

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);

        long replikertBehandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);

        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);
        assertThat(replikertBehandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerErAvsluttet_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteVedtak() {
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

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleFattetIgår.getId())).thenReturn(behandlingsresultatBleFattetIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleFattetIdag.getId())).thenReturn(behandlingsresultatBleFattetIdag);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleFattetIdag, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerErAvsluttetSisteHarIkkeVedtak_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteVedtak() {
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

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleFattetIgår.getId())).thenReturn(behandlingsresultatFattetIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatFattetIdag.getId())).thenReturn(behandlingsresultatFattetIdag);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleFattetIgår, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingHarIkkeVedtak_replikerUtenBehandlingsresultatFraSistOppdaterteBehandling() {
        Fagsak fagsak = lagFagsakMedBruker();
        var behandling = lagBehandling(2L, SOEKNAD, AVSLUTTET, Instant.now());
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), null, null);

        fagsak.setBehandlinger(List.of(behandling));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingMedNyttBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingsresultat.getId())).thenReturn(behandlingsresultat);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingMedNyttBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerAvTypeSed_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteUnntak() {
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

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIgår.getId())).thenReturn(behandlingsresultatRegistrertIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIdag.getId())).thenReturn(behandlingsresultatRegistrertIdag);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleRegistrertIdag, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerAvTypeSedSisteIkkeRegistrertUnntak_nyBehandlingOpprettetNyVurderingReplikerFraSistRegistrerteUnntak() {
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

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIgår.getId())).thenReturn(behandlingsresultatRegistrertIgår);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrertIdag.getId())).thenReturn(behandlingsresultatRegistrertIdag);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleRegistrertIgår, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingerAvTypeSedHarIkkeRegistrertUnntakt_replikerUtenBehandlingsresultatFraSistOppdaterteBehandling() {
        Fagsak fagsak = lagFagsakMedBruker();
        var behandling = lagBehandling(2L, SED, AVSLUTTET, Instant.now());
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), null, null);

        fagsak.setBehandlinger(List.of(behandling));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingMedNyttBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingMedNyttBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_kanRevurdereSEDEtterAvsluttetStatus_erUtpekingNorgeAvvist() {
        Fagsak fagsak = lagFagsakMedBruker();
        Instant idag = Instant.now();
        Behandling behandlingSomBleRegistrert = lagBehandling(2L, SED, AVSLUTTET, idag);
        behandlingSomBleRegistrert.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        Behandlingsresultat behandlingsresultatRegistrert = lagBehandlingsresultat(behandlingSomBleRegistrert, idag, null, UTPEKING_NORGE_AVVIST);
        fagsak.setBehandlinger(List.of(behandlingSomBleRegistrert));
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingSomBleRegistrert.getId())).thenReturn(behandlingsresultatRegistrert);
        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);

        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandlingSomBleRegistrert, Behandlingstyper.NY_VURDERING);
        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingerAvTypeAnnetEnnSedOgSøknad_replikerUtenBehandlingsresultatFraSistOppdaterteBehandling() {
        Fagsak fagsak = lagFagsakMedBruker();

        var behandling = lagBehandling(2L, KLAGE, AVSLUTTET, Instant.now());
        var behandlingsresultat = lagBehandlingsresultat(behandling, Instant.now(), null, null);

        fagsak.setBehandlinger(List.of(behandling));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(3L);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(behandlingService.replikerBehandlingMedNyttBehandlingsresultat(any(), any())).thenReturn(replikertBehandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(SAKSNUMMER);
        verify(behandlingService).replikerBehandlingMedNyttBehandlingsresultat(behandling, Behandlingstyper.NY_VURDERING);

        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }


    @Test
    void ferdigbehandleSak_saksstatusOPPRETTET_lagrerKorrekt() {
        var fagsak = lagFagsak();
        var behandling = lagBehandling(1L, null, null, null);
        behandling.setFagsak(fagsak);
        fagsak.getBehandlinger().add(behandling);
        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.OPPRETTET);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        ArgumentCaptor<Fagsak> fagsakArgumentCaptor = ArgumentCaptor.forClass(Fagsak.class);


        fagsakService.ferdigbehandleSak(SAKSNUMMER);


        verify(behandlingService).avsluttBehandling(behandling.getId());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(behandling.getId(), FERDIGBEHANDLET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
        verify(fagsakRepo).save(fagsakArgumentCaptor.capture());
        assertThat(fagsakArgumentCaptor.getValue().getStatus()).isEqualTo(Saksstatuser.AVSLUTTET);
    }

    @Test
    void ferdigbehandleSak_saksstatusAnnetEnnOPPRETTET_lagrerKorrekt() {
        var fagsak = lagFagsak();
        fagsak.setStatus(Saksstatuser.LOVVALG_AVKLART);
        var behandling = lagBehandling(1L, null, null, null);
        behandling.setFagsak(fagsak);
        fagsak.getBehandlinger().add(behandling);
        assertThat(fagsak.getStatus()).isNotEqualTo(Saksstatuser.OPPRETTET);

        when(fagsakRepo.findBySaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        ArgumentCaptor<Fagsak> fagsakArgumentCaptor = ArgumentCaptor.forClass(Fagsak.class);


        fagsakService.ferdigbehandleSak(SAKSNUMMER);


        verify(behandlingService).avsluttBehandling(behandling.getId());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(behandling.getId(), FERDIGBEHANDLET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
        verify(fagsakRepo).save(fagsakArgumentCaptor.capture());
        assertThat(fagsakArgumentCaptor.getValue().getStatus()).isEqualTo(fagsak.getStatus());
    }

    private Fagsak lagFagsakMedAktørforMyndighet() {
        Fagsak fagsak = lagFagsak();

        Aktoer aktoer = new Aktoer();
        aktoer.setInstitusjonId("Gammel institusjonsid");
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        fagsak.setAktører(new HashSet<>(Collections.singleton(aktoer)));
        return fagsak;
    }

    private Fagsak lagFagsakMedBruker() {
        Fagsak fagsak = lagFagsak();

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId("12312");
        aktoer.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Set.of(aktoer));
        return fagsak;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setType(EU_EOS);
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
