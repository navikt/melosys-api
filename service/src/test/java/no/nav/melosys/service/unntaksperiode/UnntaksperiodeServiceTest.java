package no.nav.melosys.service.unntaksperiode;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnntaksperiodeServiceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingService behandlingService;

    private UnntaksperiodeService unntaksperiodeService;

    private Behandling behandling = new Behandling();

    @Before
    public void setUp() throws IkkeFunnetException {
        unntaksperiodeService = new UnntaksperiodeService(behandlingService, oppgaveService, prosessinstansService);
        behandling.setFagsak(new Fagsak());
        behandling.setType(Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
    }

    @Test(expected = FunksjonellException.class)
    public void godkjennPeriode_behandlingAvsluttet_forventException() throws FunksjonellException, TekniskException {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        unntaksperiodeService.godkjennPeriode(1L);
    }

    @Test(expected = FunksjonellException.class)
    public void godkjennPeriode_feilBehandlingstype_forventException() throws FunksjonellException, TekniskException {
        behandling.setType(Behandlingstyper.SOEKNAD);
        unntaksperiodeService.godkjennPeriode(1L);
    }

    public void godkjennPeriode_korrektStatusOgType_verifiserKall() throws FunksjonellException, TekniskException {
        unntaksperiodeService.godkjennPeriode(1L);
        verify(oppgaveService).ferdigstillOppgave(any());
    }

    public void behandlingUnderAvklaring_behandlingsstatusOppdateres() throws FunksjonellException, TekniskException {
        unntaksperiodeService.behandlingUnderAvklaring(1L);
        verify(prosessinstansService).opprettProsessinstansUnntaksperiodeUnderAvklaring(any(Behandling.class));
        verify(behandlingService).oppdaterStatus(anyLong(), Behandlingsstatus.AVVENT_DOK_UTL);
    }

    @Test
    public void ikkeGodkjennPeriode_medBegrunnelser_ingenFeil() throws Exception {
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null);
        verify(prosessinstansService).opprettProsessinstansUnntaksperiodeAvvist(any(), anySet(), any());
    }

    @Test
    public void ikkeGodkjennPeriode_ingenBegrunnelser_forventException() throws Exception {
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Ingen begrunnelser for avlag av periode");
        unntaksperiodeService.ikkeGodkjennPeriode(1l, new HashSet<>(), null);
    }

    @Test
    public void ikkeGodkjennPeriode_begrunnelseAnnetIngenFritekst_forventException() throws Exception {
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        begrunnelser.add(Ikke_godkjent_begrunnelser.ANNET.getKode());

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Begrunnelse " + Ikke_godkjent_begrunnelser.ANNET + " krever fritekst!");
        unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null);
    }

    private Behandling hentBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        return behandling;
    }
}