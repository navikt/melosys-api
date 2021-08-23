package no.nav.melosys.service.sak;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private PersondataFasade tps;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private final FakeUnleash unleash = new FakeUnleash();

    private FagsakService fagsakService;

    private static final EasyRandom random = new EasyRandom(getRandomConfig());

    private static EasyRandomParameters getRandomConfig() {
        return new EasyRandomParameters().collectionSizeRange(1, 4)
            .randomize(PeriodeDto.class, () -> new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))
            .stringLengthRange(2, 4);
    }

    @BeforeEach
    public void setUp() {
        fagsakService = new FagsakService(fagsakRepo, behandlingService, kontaktopplysningService, oppgaveService,
            tps, prosessinstansService, behandlingsresultatService, medlPeriodeService, unleash);
        unleash.enableAll();
    }

    @Test
    void hentFagsak() {
        String saksnummer = "saksnummer";
        when(fagsakRepo.findBySaksnummer(anyString())).thenReturn(Optional.of(new Fagsak()));
        fagsakService.hentFagsak(saksnummer);
        verify(fagsakRepo).findBySaksnummer(eq(saksnummer));
    }

    @Test
    void hentFagsakerMedAktør() {
        when(tps.hentAktørIdForIdent(any())).thenReturn("AKTOER_ID");
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "FNR");
        verify(fagsakRepo).findByRolleAndAktør(eq(Aktoersroller.BRUKER), eq("AKTOER_ID"));
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
    void bestillNySakOgBehandling_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.ØVRIGE_SED_MED);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(opprettSakDto.getOppgaveID()))).thenReturn(oppgave);
        fagsakService.bestillNySakOgBehandling(opprettSakDto);
        verify(prosessinstansService).opprettProsessinstansNySak(eq(oppgave.getJournalpostId()), eq(opprettSakDto), eq(Behandlingstyper.SED));
    }

    @Test
    void bestillNySakOgBehandling_sakstypeFtrlFeatureToggleDisabled_kasterException() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_I_UTLANDET);
        unleash.disableAll();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Kan ikke opprette ny sak med");
    }

    @Test
    void bestillNySakOgBehandling_oppgaveIdMangler_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setOppgaveID("");
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("OppgaveID mangler.");
    }

    @Test
    void bestillNySakOgBehandling_utenJournalpostID_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(null).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(opprettSakDto.getOppgaveID()))).thenReturn(oppgave);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("mangler journalpost med søknad");
    }

    @Test
    void bestillNySakOgBehandling_oppgaveTypeUgyldig_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.JFR).setJournalpostId("33").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(opprettSakDto.getOppgaveID()))).thenReturn(oppgave);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("kan ikke opprettes på bakgrunn av oppgave med type");
    }

    @Test
    void validerOpprettSakDto_manglerBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(null);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Behandlingstema");
    }

    @Test
    void validerOpprettSakDto_nullSøknad_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setSoknadDto(null);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("må ikke være null");
    }

    @Test
    void validerOpprettSakDto_søknadUtenFom_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().getPeriode().setFom(null);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("fra og med dato");
    }

    @Test
    void validerOpprettSakDto_søknadUtenLand_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().getLand().getLandkoder().clear();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("land");
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
            .medBehandlingstype(Behandlingstyper.SOEKNAD)
            .medBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medInitierendeJournalpostId(initierendeJournalpostId)
            .medInitierendeDokumentId(initierendeDokumentId)
            .medArbeidsgiver("arbeidsgiver")
            .medFullmektig(new Fullmektig("orgnr", Representerer.ARBEIDSGIVER))
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        verify(fagsakRepo).save(any(Fagsak.class));
        verify(behandlingService).nyBehandling(any(), eq(Behandlingsstatus.OPPRETTET), eq(Behandlingstyper.SOEKNAD),
            eq(Behandlingstema.UTSENDT_ARBEIDSTAKER), eq(initierendeJournalpostId), eq(initierendeDokumentId));
        assertThat(fagsak.getBehandlinger()).isNotEmpty();
        assertThat(fagsak.getType()).isEqualTo(Sakstyper.EU_EOS);
        Aktoer forventetFullmektig = new Aktoer();
        forventetFullmektig.setFagsak(fagsak);
        forventetFullmektig.setRolle(Aktoersroller.REPRESENTANT);
        forventetFullmektig.setOrgnr("orgnr");
        forventetFullmektig.setRepresenterer(Representerer.ARBEIDSGIVER);
        assertThat(fagsak.hentRepresentant(Representerer.ARBEIDSGIVER)).isPresent().get()
            .isEqualToComparingFieldByField(forventetFullmektig);
    }

    @Test
    void nyFagsakOgBehandling_kontaktPersonFinnes_KontaktOpplysningOpprettes() {
        Kontaktopplysning kontaktopplysning = Kontaktopplysning.av("RepresentantOrgnr", "Kontaktperson", "Telefon");
        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID("123456789")
            .medBehandlingstype(Behandlingstyper.SOEKNAD)
            .medKontaktopplysninger(List.of(kontaktopplysning)).build();

        fagsakService.nyFagsakOgBehandling(opprettSakRequest);

        verify(kontaktopplysningService).lagEllerOppdaterKontaktopplysning(
            any(), eq("RepresentantOrgnr"), eq(null), eq("Kontaktperson"), eq("Telefon")
        );
    }

    @Test
    void avsluttSakSomBortfalt_harFagsakMedFlereBehandlinger_AvslutterAlleBehandlingerOgSetterFagsakstatusTilHENLAGT_BORTFALT() {
        String saksnummer = "saksnummer";
        Fagsak fagsak = lagFagsak(saksnummer);
        Behandling førsteBehandling = new Behandling();
        førsteBehandling.setId(1L);
        førsteBehandling.setStatus(Behandlingsstatus.OPPRETTET);
        Behandling andreBehandling = new Behandling();
        andreBehandling.setId(2L);
        andreBehandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        fagsak.setBehandlinger(Arrays.asList(førsteBehandling, andreBehandling));
        fagsakService.avsluttSakSomBortfalt(fagsak);

        verify(fagsakRepo).save(fagsak);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.HENLEGGELSE);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(2L, Behandlingsresultattyper.HENLEGGELSE);
        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.HENLAGT_BORTFALT);
        assertThat(fagsak.getBehandlinger()).allSatisfy(behandling -> assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(saksnummer);
    }

    @Test
    void avsluttFagsakOgBehandlingValiderBehandlingstema_behtemaIkkeYrkesaktiv_blirAvsluttet() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.LOVVALG_AVKLART);
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
    }

    @Test
    void avsluttFagsakOgBehandlingValiderBehandlingstype_behtemaTrygdetid_blirAvsluttet() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.SED);
        behandling.setTema(Behandlingstema.TRYGDETID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.AVSLUTTET);
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
    }

    @Test
    void avsluttFagsakOgBehandlingValiderBehandlingstype_behtemaUtsendtArbeidstaker_kasterException() {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling))
            .withMessageContaining("kan ikke avsluttes manuelt");
    }

    @Test
    void leggTilFjernAktørerForMyndighet() {
        String saksnummer = "1234";
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet(saksnummer, "Gammel institusjonsid");
        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(Optional.of(eksisterendeFagsak));

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheter(saksnummer, nyeInstitusjonsIder);

        ArgumentCaptor<Fagsak> captor = ArgumentCaptor.forClass(Fagsak.class);
        verify(fagsakRepo).save(captor.capture());
        Fagsak oppdaterFagsak = captor.getValue();
        assertThat(oppdaterFagsak.getAktører().stream()
            .map(Aktoer::getInstitusjonId)
            .collect(Collectors.toList())).containsOnlyElementsOf(nyeInstitusjonsIder);
    }

    @Test
    void oppdaterMyndigheter_harBruker_fjernerIkkeBruker() {
        String saksnummer = "1234";
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet(saksnummer, "Gammel institusjonsid");
        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(Optional.of(eksisterendeFagsak));

        Aktoer bruker = new Aktoer();
        bruker.setFagsak(eksisterendeFagsak);
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("1234");
        eksisterendeFagsak.getAktører().add(bruker);

        List<String> nyeInstitusjonsIder = Collections.singletonList("Ny institusjonsid");
        fagsakService.oppdaterMyndigheter(saksnummer, nyeInstitusjonsIder);

        ArgumentCaptor<Fagsak> captor = ArgumentCaptor.forClass(Fagsak.class);
        verify(fagsakRepo).save(captor.capture());
        Fagsak oppdaterFagsak = captor.getValue();

        assertThat(oppdaterFagsak.getAktører())
            .extracting(Aktoer::getRolle, Aktoer::getAktørId, Aktoer::getInstitusjonId)
            .containsExactlyInAnyOrder(
                tuple(Aktoersroller.BRUKER, "1234", null),
                tuple(Aktoersroller.MYNDIGHET, null, "Ny institusjonsid")
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
        verify(fagsakRepo).save(eq(fagsak));
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
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
        Fagsak fagsak = lagFagsakMedBruker(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.ENDRET_PERIODE);
        behandling.setEndretDato(Instant.now());
        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(Optional.of(fagsak));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(saksnummer))
            .withMessageContaining("Kan ikke revurdere en behandling av type " + Behandlingstyper.ENDRET_PERIODE.getBeskrivelse());
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivIkkeArt16_kastException() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(new Behandlingsresultat());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(saksnummer))
            .withMessageContaining("Kan ikke revurdere en aktiv behandling");
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningIkkeSendt_kastException() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        fagsak.setBehandlinger(List.of(behandling));

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(false);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> fagsakService.opprettNyVurderingBehandling(saksnummer))
            .withMessageContaining("Kan ikke revurdere en aktiv behandling");
    }

    @Test
    void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningSendt_nyBehandlingOpprettet() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        behandling.setType(Behandlingstyper.SOEKNAD);
        fagsak.setBehandlinger(List.of(behandling));

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(behandlingsresultat);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any(), any())).thenReturn(replikertBehandling);

        long replikertBehandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(eq(behandling), eq(Behandlingsstatus.OPPRETTET), eq(behandling.getType()));
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
        assertThat(replikertBehandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    void opprettNyVurderingBehandling_toBehandlingerErAvsluttet_nyBehandlingOpprettetTypeNyVurderingReplikerFraSistOppdaterte() {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker(saksnummer);

        Behandling sistOppdaterteBehandling = new Behandling();
        sistOppdaterteBehandling.setId(1L);
        sistOppdaterteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        sistOppdaterteBehandling.setType(Behandlingstyper.SOEKNAD);
        sistOppdaterteBehandling.setEndretDato(Instant.now());

        Behandling senestOppdaterteBehandling = new Behandling();
        senestOppdaterteBehandling.setId(9999L);
        senestOppdaterteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        senestOppdaterteBehandling.setType(Behandlingstyper.SOEKNAD);
        senestOppdaterteBehandling.setEndretDato(Instant.now().minusSeconds(3600L));

        fagsak.setBehandlinger(List.of(sistOppdaterteBehandling, senestOppdaterteBehandling));

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        anmodningsperiode.setMedlPeriodeID(123L);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(2L);

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(sistOppdaterteBehandling.getId()))).thenReturn(behandlingsresultat);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any(), any())).thenReturn(replikertBehandling);

        long behandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(eq(sistOppdaterteBehandling), eq(Behandlingsstatus.OPPRETTET), eq(Behandlingstyper.NY_VURDERING));
        verify(medlPeriodeService).avvisPeriode(eq(anmodningsperiode.getMedlPeriodeID()));
        assertThat(behandlingID).isEqualTo(replikertBehandling.getId());
    }

    private Fagsak lagFagsakMedAktørforMyndighet(String saksnummer, String id) {
        Fagsak fagsak = lagFagsak(saksnummer);

        Aktoer aktoer = new Aktoer();
        aktoer.setInstitusjonId(id);
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(Aktoersroller.MYNDIGHET);
        fagsak.setAktører(new HashSet<>(Collections.singleton(aktoer)));
        return fagsak;
    }

    private Fagsak lagFagsakMedBruker(String saksnummer) {
        Fagsak fagsak = lagFagsak(saksnummer);

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
}
