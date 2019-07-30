package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IverksettVedtakValideringTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private IverksettVedtakValidering agent;

    private Prosessinstans p;

    @Before
    public void setUp() throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getLovvalgsperioder().add(new Lovvalgsperiode());
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        agent = new IverksettVedtakValidering(behandlingsresultatService);

        p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");

        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.getKode());
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        agent.utfør(p);

        assertThat(p.getSteg()).isEqualTo(IV_OPPDATER_RESULTAT);
    }

    @Test
    public void utfør_feilProsessType() throws FunksjonellException, TekniskException {
        p.setType(ProsessType.OPPFRISKNING);

        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("ProsessType " + ProsessType.OPPFRISKNING + " er ikke støttet.");

        agent.utfør(p);
    }

    @Test
    public void utfør_manglerBehandlingsresultatType() throws FunksjonellException, TekniskException {
        p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(2L);
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("BehandlingsResultatType er ikke oppgitt.");

        agent.utfør(p);
    }

    @Test
    public void utfør_iverksettVedtakManglerLovvalgsperiode_feiler() throws FunksjonellException, TekniskException {
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(new Behandlingsresultat());

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Lovvalgsperiode mangler for behandlingsresultat");

        agent.utfør(p);
    }
}