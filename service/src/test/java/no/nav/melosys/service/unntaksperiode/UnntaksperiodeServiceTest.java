package no.nav.melosys.service.unntaksperiode;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnntaksperiodeServiceTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private UnntaksperiodeService unntaksperiodeService;

    private final Behandling behandling = new Behandling();

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        unntaksperiodeService = new UnntaksperiodeService(behandlingService, behandlingsresultatService, lovvalgsperiodeService, oppgaveService, prosessinstansService);
        behandling.setId(1L);
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("MEL-123hei");
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
    }

    @Test
    void godkjennPeriode_behandlingAvsluttet_forventException() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.godkjennPeriode(1L,  false))
            .withMessageContaining("er inaktiv");
    }

    @Test
    void godkjennPeriode_feilBehandlingstype_forventException() {
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.godkjennPeriode(1L,  false))
            .withMessageContaining("ikke av tema");
    }

    @Test
    void godkjennPeriode_korrektStatusOgType_verifiserKall() throws FunksjonellException, TekniskException {
        Saksopplysning sedSaksopplysning = new Saksopplysning();
        sedSaksopplysning.setType(SaksopplysningType.SEDOPPL);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode());
        sedSaksopplysning.setDokument(sedDokument);
        behandling.getSaksopplysninger().add(sedSaksopplysning);

        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(new Behandlingsresultat());

        unntaksperiodeService.godkjennPeriode(1L, false);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(behandling.getFagsak().getSaksnummer()));
    }

    @Test
    void ikkeGodkjennPeriode_medBegrunnelser_ingenFeil() throws Exception {
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null);
        verify(prosessinstansService).opprettProsessinstansUnntaksperiodeAvvist(any(), anySet(), any());
    }

    @Test
    void ikkeGodkjennPeriode_ingenBegrunnelser_forventException() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.ikkeGodkjennPeriode(1L, Set.of(), null))
            .withMessageContaining("Ingen begrunnelser");
    }

    @Test
    void ikkeGodkjennPeriode_begrunnelseAnnetIngenFritekst_forventException() {
        Set<String> begrunnelser = new HashSet<>();
        begrunnelser.add(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode());
        begrunnelser.add(Ikke_godkjent_begrunnelser.ANNET.getKode());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null))
            .withMessageContaining("krever fritekst");
    }
}