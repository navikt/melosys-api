package no.nav.melosys.saksflyt.steg.sed;

import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeGodkjenning;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BestemBehandlingsmåteSedTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private UnntaksperiodeService unntaksperiodeService;

    private BestemBehandlingsmåteSed bestemBehandlingsmåteSed;

    private final Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @BeforeEach
    public void setUp() {
        bestemBehandlingsmåteSed = new BestemBehandlingsmåteSed(behandlingService, behandlingsresultatService, oppgaveService, unntaksperiodeService);
        prosessinstans.setBehandling(behandling);
        behandling.setId(234L);

        behandling.setFagsak(FagsakTestFactory.builder().medBruker().build());

        when(behandlingService.hentBehandlingMedSaksopplysninger(eq(behandling.getId()))).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_temaRegistreringUnntakIngenTreffIRegister_prosessOpprettes() {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        bestemBehandlingsmåteSed.utfør(prosessinstans);

        UnntaksperiodeGodkjenning forventetUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .varsleUtland(false)
            .fritekst(null)
            .build();
        verify(unntaksperiodeService).godkjennPeriode(eq(behandling.getId()), eq(forventetUnntaksperiodeGodkjenning));
    }

    @Test
    void utfør_temaRegistreringUnntakMedTreffIRegister_oppgaveOpprettes() {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        Kontrollresultat kontrollresultat = new Kontrollresultat();
        kontrollresultat.setBegrunnelse(Kontroll_begrunnelser.FEIL_I_PERIODEN);
        behandlingsresultat.setKontrollresultater(Set.of(kontrollresultat));

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        bestemBehandlingsmåteSed.utfør(prosessinstans);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any(), any());
    }
}
