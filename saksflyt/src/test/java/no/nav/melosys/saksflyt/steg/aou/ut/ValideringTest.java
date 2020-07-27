package no.nav.melosys.saksflyt.steg.aou.ut;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AOU_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FEILET_MASKINELT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ValideringTest {

    private Validering agent;
    private Prosessinstans p;

    @Before
    public void setUp() {
        agent = new Validering();

        p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");
    }

    @Test
    public void utfoerSteg() throws TekniskException {
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(AOU_OPPDATER_RESULTAT);
    }

    @Test
    public void utfoerSteg_feilProsessType() throws TekniskException {
        p.setType(ProsessType.MOTTAK_SED);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_manglerSaksbehandler_feiler() throws TekniskException {
        p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);

        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }
}