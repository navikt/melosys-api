package no.nav.melosys.service.sak;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FagsakServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private FagsakRepository fagsakRepo;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private KontaktopplysningService kontaktopplysningService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private TpsFasade tps;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private FagsakService fagsakService;

    private static EasyRandom random = new EasyRandom(getRandomConfig());

    private static EasyRandomParameters getRandomConfig() {
        return new EasyRandomParameters().collectionSizeRange(1, 4)
            .stringLengthRange(2, 4);
    }

    @Before
    public void setUp() {
        fagsakService = new FagsakService(fagsakRepo, behandlingService, kontaktopplysningService, oppgaveService,
            tps, prosessinstansService, behandlingsresultatService, medlPeriodeService);
    }

    @Test
    public void hentFagsak() throws IkkeFunnetException {
        String saksnummer = "saksnummer";
        when(fagsakRepo.findBySaksnummer(anyString())).thenReturn(new Fagsak());
        fagsakService.hentFagsak(saksnummer);
        verify(fagsakRepo).findBySaksnummer(eq(saksnummer));
    }

    @Test
    public void hentFagsakerMedAktør() throws IkkeFunnetException {
        when(tps.hentAktørIdForIdent(any())).thenReturn("AKTOER_ID");
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "FNR");
        verify(fagsakRepo).findByRolleAndAktør(eq(Aktoersroller.BRUKER), eq("AKTOER_ID"));
    }

    @Test
    public void lagre() {
        Fagsak fagsak = lagFagsak("12345");
        fagsakService.lagre(fagsak);
        verify(fagsakRepo).save(fagsak);
        assertThat(fagsak).isNotNull();
        assertThat(fagsak.getSaksnummer()).isNotEmpty();
    }

    @Test
    public void bestillNySakOgBehandling_oppretterProsess() throws FunksjonellException, TekniskException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(opprettSakDto.getOppgaveID()))).thenReturn(oppgave);
        fagsakService.bestillNySakOgBehandling(opprettSakDto);
        verify(prosessinstansService).opprettProsessinstansNySak(eq(oppgave.getJournalpostId()), eq(opprettSakDto));
    }

    @Test
    public void bestillNySakOgBehandling_oppgaveIdMangler_feiler() throws FunksjonellException, TekniskException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setOppgaveID("");
        expectedException.expect(FunksjonellException.class);
        fagsakService.bestillNySakOgBehandling(opprettSakDto);
    }

    @Test
    public void bestillNySakOgBehandling_utenJournalpostID_feiler() throws FunksjonellException, TekniskException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(null).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(opprettSakDto.getOppgaveID()))).thenReturn(oppgave);
        expectedException.expect(FunksjonellException.class);
        fagsakService.bestillNySakOgBehandling(opprettSakDto);
    }

    @Test
    public void bestillNySakOgBehandling_oppgaveTypeUgyldig_feiler() throws FunksjonellException, TekniskException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SED).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(opprettSakDto.getOppgaveID()))).thenReturn(oppgave);
        expectedException.expect(FunksjonellException.class);
        fagsakService.bestillNySakOgBehandling(opprettSakDto);
    }

    @Test
    public void validerOpprettSakDto_manglerBehandlingstype_feiler() throws FunksjonellException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(null);
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("behandlingstema");
        fagsakService.validerOpprettSakDto(opprettSakDto);
    }

    @Test
    public void validerOpprettSakDto_nullSøknad_feiler() throws FunksjonellException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setSoknadDto(null);
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("null");
        fagsakService.validerOpprettSakDto(opprettSakDto);
    }

    @Test
    public void validerOpprettSakDto_søknadUtenFom_feiler() throws FunksjonellException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().getPeriode().setFom(null);
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("fra og med dato");
        fagsakService.validerOpprettSakDto(opprettSakDto);
    }

    @Test
    public void validerOpprettSakDto_søknadUtenLand_feiler() throws FunksjonellException {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().getLand().clear();
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("land");
        fagsakService.validerOpprettSakDto(opprettSakDto);
    }

    @Test
    public void nyFagsakOgBehandling() throws FunksjonellException {
        Behandling behandling = mock(Behandling.class);
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";
        doReturn(behandling).when(behandlingService).nyBehandling(any(), any(), any(), any(), anyString(), anyString());

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID("AKTOER_ID").medAktørID("123456789")
            .medBehandlingstype(Behandlingstyper.SOEKNAD).medBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medInitierendeJournalpostId(initierendeJournalpostId).medInitierendeDokumentId(initierendeDokumentId).build();
        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        verify(fagsakRepo).save(any(Fagsak.class));
        verify(behandlingService).nyBehandling(any(), eq(Behandlingsstatus.OPPRETTET), eq(Behandlingstyper.SOEKNAD), eq(Behandlingstema.UTSENDT_ARBEIDSTAKER), eq(initierendeJournalpostId), eq(initierendeDokumentId));
        assertThat(fagsak.getBehandlinger()).isNotEmpty();
        assertThat(fagsak.getType()).isEqualTo(Sakstyper.UKJENT);
    }

    @Test
    public void nyFagsakOgBehandling_kontaktPersonFinnes_KontaktOpplysningOpprettes() throws FunksjonellException {
        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID("AKTOER_ID").medAktørID("123456789")
            .medBehandlingstype(Behandlingstyper.SOEKNAD).medRepresentant("RepresentantOrgnr").medRepresentantKontaktperson("Kontaktperson").build();

        fagsakService.nyFagsakOgBehandling(opprettSakRequest);

        verify(kontaktopplysningService).lagEllerOppdaterKontaktopplysning(any(), eq("RepresentantOrgnr"), eq(null), eq("Kontaktperson"));
    }

    @Test
    public void henleggFagsakMedToBehandlingerHenterSisteBehandling() throws TekniskException, FunksjonellException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Behandling andreBehandling = new Behandling();
        Fagsak andreBehandlingFagsak = new Fagsak();
        andreBehandlingFagsak.setSaksnummer("987654321");
        andreBehandling.setFagsak(andreBehandlingFagsak);
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId);

        String fritekst = "Fri tale";
        fagsakService.henleggFagsak(saksnummer, "ANNET", fritekst);

        verify(prosessinstansService).opprettProsessinstansHenleggSak(andreBehandling, Henleggelsesgrunner.ANNET, fritekst);

        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(eq(førsteBehandling), any(), anyString());

        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(andreBehandlingFagsak.getSaksnummer()));
    }

    @Test
    public void henleggFagsakMedToBehandlingerHenterFørsteBehandlingHvisSisteErAvsluttet() throws TekniskException, FunksjonellException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Fagsak førsteBehandlingFagsak = new Fagsak();
        førsteBehandlingFagsak.setSaksnummer("987654321");
        førsteBehandling.setFagsak(førsteBehandlingFagsak);
        Behandling andreBehandling = new Behandling();
        andreBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId);

        String fritekst = "Fri tale";
        fagsakService.henleggFagsak(saksnummer, "ANNET", fritekst);

        verify(prosessinstansService).opprettProsessinstansHenleggSak(førsteBehandling, Henleggelsesgrunner.ANNET, fritekst);

        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(eq(andreBehandling), any(), anyString());

        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(førsteBehandlingFagsak.getSaksnummer()));
    }

    @Test
    public void henleggFagsakMedToBehandlingerKasterExceptionNårIkkeGyldigHenleggelsesgrunn() throws TekniskException, FunksjonellException {
        String saksnummer = "123456789";
        initierFagsakMedToBehandlinger(new Fagsak(), saksnummer, new Behandling(), new Behandling(), 999L, 234L);

        expectedException.expect(TekniskException.class);

        fagsakService.henleggFagsak(saksnummer, "UGYLDIGKODE", "Fri tale");

        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(any(), any(), anyString());

        verify(oppgaveService, never()).ferdigstillOppgaveMedSaksnummer(anyString());
    }

    private void initierFagsakMedToBehandlinger(Fagsak fagsak, String saksnummer, Behandling førsteBehandling, Behandling andreBehandling, long førsteBehandlingId, long andreBehandlingId) {
        førsteBehandling.setRegistrertDato(Instant.parse("2000-10-10T10:12:35Z"));
        førsteBehandling.setId(førsteBehandlingId);
        Instant registrertDatoForSisteBehandling = Instant.parse("2010-11-11T10:12:35Z");
        andreBehandling.setId(andreBehandlingId);
        andreBehandling.setRegistrertDato(registrertDatoForSisteBehandling);
        fagsak.setBehandlinger(Arrays.asList(førsteBehandling, andreBehandling));

        doReturn(fagsak).when(fagsakRepo).findBySaksnummer(saksnummer);
    }

    @Test
    public void avsluttSakSomBortfalt_harFagsakMedFlereBehandlinger_AvslutterAlleBehandlingerOgSetterFagsakstatusTilHENLAGT_BORTFALT() throws FunksjonellException, TekniskException {
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
    public void avsluttFagsakOgBehandlingValiderBehandlingstema_behtemaIkkeYrkesaktiv_blirAvsluttet() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        fagsak.setBehandlinger(List.of(behandling));
        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.LOVVALG_AVKLART);
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
    }

    @Test
    public void avsluttFagsakOgBehandlingValiderBehandlingstype_behtemaTrygdetid_blirAvsluttet() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-123");
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.SED);
        behandling.setTema(Behandlingstema.TRYGDETID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(List.of(behandling));
        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling);

        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.AVSLUTTET);
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
    }

    @Test
    public void avsluttFagsakOgBehandlingValiderBehandlingstype_behtemaUtsendtArbeidstaker_kasterException() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("kan ikke avsluttes manuelt");

        fagsakService.avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, behandling);
    }

    @Test
    public void leggTilFjernAktørerForMyndighet() throws IkkeFunnetException {
        String saksnummer = "1234";
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet(saksnummer, "Gammel institusjonsid");
        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(eksisterendeFagsak);

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
    public void oppdaterMyndigheter_harBruker_fjernerIkkeBruker() throws IkkeFunnetException {
        String saksnummer = "1234";
        Fagsak eksisterendeFagsak = lagFagsakMedAktørforMyndighet(saksnummer, "Gammel institusjonsid");
        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(eksisterendeFagsak);

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
    public void avsluttFagsakOgBehandling() throws IkkeFunnetException, TekniskException {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(List.of(behandling));

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART);
        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.LOVVALG_AVKLART);
        verify(fagsakRepo).save(eq(fagsak));
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
    }

    @Test
    public void opprettNyVurderingBehandling_behandlingstypeEndretPeriode_kastException() throws FunksjonellException, TekniskException {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.ENDRET_PERIODE);
        behandling.setEndretDato(Instant.now());
        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(fagsak);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke revurdere en behandling av type " + Behandlingstyper.ENDRET_PERIODE.getBeskrivelse());

        fagsakService.opprettNyVurderingBehandling(saksnummer);
    }

    @Test
    public void opprettNyVurderingBehandling_behandlingErAktivIkkeArt16_kastException() throws FunksjonellException, TekniskException {
        final String saksnummer = "MEL-1";
        Fagsak fagsak = lagFagsakMedBruker(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(List.of(behandling));

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(new Behandlingsresultat());

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke revurdere en aktiv behandling");

        fagsakService.opprettNyVurderingBehandling(saksnummer);
    }

    @Test
    public void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningIkkeSendt_kastException() throws FunksjonellException, TekniskException {
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

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(behandlingsresultat);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke revurdere en aktiv behandling");

        fagsakService.opprettNyVurderingBehandling(saksnummer);
    }

    @Test
    public void opprettNyVurderingBehandling_behandlingErAktivErArt16AnmodningSendt_nyBehandlingOpprettet() throws FunksjonellException, TekniskException {
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

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(behandlingsresultat);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(), any(), any())).thenReturn(replikertBehandling);

        long replikertBehandlingID = fagsakService.opprettNyVurderingBehandling(saksnummer);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(eq(behandling), eq(Behandlingsstatus.OPPRETTET), eq(behandling.getType()));
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
        assertThat(replikertBehandlingID).isEqualTo(replikertBehandling.getId());
    }

    @Test
    public void opprettNyVurderingBehandling_toBehandlingerErAvsluttet_nyBehandlingOpprettetTypeNyVurderingReplikerFraSistOppdaterte() throws FunksjonellException, TekniskException {
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

        when(fagsakRepo.findBySaksnummer(eq(saksnummer))).thenReturn(fagsak);
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