package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.util.Optional;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.eessi.ruting.DefaultSedRuter;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultSedRuterTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;

    private final FakeUnleash unleash = new FakeUnleash();

    private DefaultSedRuter defaultSedRuter;

    private static final String SAKSNUMMER = "MEL-!!!";
    private static final Long GSAK_SAKSNUMMER = 123L;

    @BeforeEach
    public void setup() {
        defaultSedRuter = new DefaultSedRuter(prosessinstansService, fagsakService, behandlingService, oppgaveService, unleash);
        unleash.enableAll();
    }

    @Test
    void bestemManuellBehandling_saksnummerFinnesIkkeErNySedINyBehandling_nesteStegOppretJfrOppg() {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A005);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        defaultSedRuter.rutSedTilBehandling(prosessinstans, null);

        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

    @Test
    void bestemManuellBehandling_X009PurringSaksnummerOgFagsakEksisterer_oppdatererPrioritet() {

        final String oppgaveId = "333";
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.X009);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Oppgave oppgave = new Oppgave.Builder().setOppgaveId(oppgaveId).build();
        Fagsak fagsak = hentFagsak();

        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(Optional.of(oppgave));

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);

        assertThat(prosessinstans.getBehandling()).isNotNull();
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(fagsak.hentAktivBehandling(), melosysEessiMelding);
        verify(behandlingService).endreStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
        verify(oppgaveService).finnÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER);
        verify(oppgaveService).oppdaterOppgave(eq(oppgaveId), any(OppgaveOppdatering.class));
    }

    @ParameterizedTest
    @EnumSource(value = SedType.class, names = {"A012", "X001", "X007"})
    void rutSedTilBehandling_SedTyperSaksnummerOgFagsakEksistererStatusMidlertidigLovvalgsbeslutning_ikkeOppdaterStatusEllerOppgave(SedType sedType) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(sedType);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Fagsak fagsak = hentFagsak();
        fagsak.hentSistOppdatertBehandling().setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);

        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));


        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);


        assertThat(prosessinstans.getBehandling()).isNotNull();
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(fagsak.hentSistAktivBehandling(), melosysEessiMelding);
        verify(behandlingService, never()).endreStatus(anyLong(), any());
        verify(oppgaveService, never()).finnÅpenBehandlingsoppgaveMedFagsaksnummer(any());
        verifyNoInteractions(oppgaveService);
    }

    @Test
    void bestemManuellBehandling_behandlingOpprettetOgSkalBehandleSED_opprettOppgave() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A004);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Fagsak fagsak = hentFagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        Behandling behandling = fagsak.hentAktivBehandling();
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(oppgaveService.lagBehandlingsoppgave(any())).thenReturn(OppgaveFactory.lagBehandlingsoppgave(behandling, LocalDate.now()));

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);

        var oppgaveCaptor = ArgumentCaptor.forClass(Oppgave.class);
        var oppgaveOppdateringArgumentCaptor = ArgumentCaptor.forClass(OppgaveOppdatering.class);
        verify(behandlingService).endreStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        verify(oppgaveService).oppdaterOppgave(any(), oppgaveOppdateringArgumentCaptor.capture());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(fagsak.hentSistAktivBehandling(), melosysEessiMelding);
        assertThat(oppgaveCaptor.getValue())
            .extracting(Oppgave::getSaksnummer)
            .isEqualTo(SAKSNUMMER);
        assertThat(oppgaveOppdateringArgumentCaptor.getValue())
            .extracting(OppgaveOppdatering::getBeskrivelse)
            .isEqualTo("Mottatt SED A004");
    }

    @Test
    void bestemManuellBehandling_behandlingAvsluttet_opprettJournalforingsOppgave() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A004);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Fagsak fagsak = hentFagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        Behandling behandling = fagsak.hentAktivBehandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));


        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);


        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
        verifyNoMoreInteractions(oppgaveService);
        verifyNoInteractions(behandlingService);
        verifyNoInteractions(prosessinstansService);
    }

    private MelosysEessiMelding hentMelosysEessiMelding(SedType sedType) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(sedType.name());
        melosysEessiMelding.setAktoerId("12321321");
        return melosysEessiMelding;
    }

    private Fagsak hentFagsak() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setTema(Behandlingstema.ØVRIGE_SED_MED);
        behandling.setType(Behandlingstyper.SOEKNAD);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.getBehandlinger().add(behandling);
        behandling.setFagsak(fagsak);
        return fagsak;
    }
}
