package no.nav.melosys.saksflyt.agent.jfr;

import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentPersonopplysningerTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository repo;

    @Mock
    private TpsFasade tpsFasade;

    private HentPersonopplysninger agent;

    @Before
    public void setUp() {
        agent = new HentPersonopplysninger(binge, repo, tpsFasade);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException, SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        String brukerID = "99999999991";
        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        when(tpsFasade.hentPerson(any())).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        verify(tpsFasade, times(1)).hentPerson(brukerID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_INNGANGSVILKÅR);
    }
}