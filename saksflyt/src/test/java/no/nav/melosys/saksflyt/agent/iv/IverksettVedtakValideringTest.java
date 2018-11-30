package no.nav.melosys.saksflyt.agent.iv;

import no.nav.melosys.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IverksettVedtakValideringTest {

    private IverksettVedtakValidering agent;

    @Before
    public void setUp() {
        agent = new IverksettVedtakValidering();
    }

    @Test
    public void utfoerSteg() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");
        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.FASTSATT_LOVVALGSLAND.getKode());

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(IV_OPPDATER_RESULTAT);
    }

    @Test
    public void utfoerSteg_feilProsessType() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.OPPFRISKNING);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_manglerSaksbehandler() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }
}