package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttTidligerePeriodeTest {

    private AvsluttTidligerePeriode avsluttTidligerePeriode;

    @Mock
    private OppdaterMedlFelles felles;

    @Before
    public void setup() {
        avsluttTidligerePeriode = new AvsluttTidligerePeriode(felles);
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.ER_OPPDATERT_SED, true);
        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        prosessinstans.setBehandling(behandling);

        avsluttTidligerePeriode.utfør(prosessinstans);

        verify(felles).avsluttTidligerMedlPeriode(eq(behandling.getFagsak()));

    }

}