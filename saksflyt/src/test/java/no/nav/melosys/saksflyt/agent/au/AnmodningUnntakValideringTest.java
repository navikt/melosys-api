package no.nav.melosys.saksflyt.agent.au;

import no.nav.melosys.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.AU_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakValideringTest {

    private AnmodningUnntakValidering agent;
    private Prosessinstans p;

    @Before
    public void setUp() {
        agent = new AnmodningUnntakValidering();

        p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.ANMODNING_UNNTAK);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");
        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.ANMODNING_OM_UNNTAK);
    }

    @Test
    public void utfoerSteg() {
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(AU_OPPDATER_RESULTAT);
    }

    @Test
    public void utfoerSteg_feilProsessType() {
        p.setType(ProsessType.OPPFRISKNING);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_manglerBehandlingsresultatType() {
        p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.ANMODNING_UNNTAK);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");

        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }
}