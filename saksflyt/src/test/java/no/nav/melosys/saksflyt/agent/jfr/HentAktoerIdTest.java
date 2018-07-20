package no.nav.melosys.saksflyt.agent.jfr;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentAktoerIdTest {

    @Mock
    private TpsFasade tpsFasade;

    private HentAktoerId agent;

    @Before
    public void setUp() {
        agent = new HentAktoerId(tpsFasade);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        String brukerID = "99999999991";
        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        when(tpsFasade.hentAktørIdForIdent(any())).thenReturn("FJERNET93");

        agent.utførSteg(p);

        verify(tpsFasade, times(1)).hentAktørIdForIdent(brukerID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_SAK_OG_BEH);
    }
}