package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sob.SobService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSakOgBehandlingAvsluttetTest {

    @Mock
    private SobService sobService;

    private OppdaterSakOgBehandlingAvsluttet oppdaterSakOgBehandlingAvsluttet;

    @Before
    public void setup() {
        oppdaterSakOgBehandlingAvsluttet = new OppdaterSakOgBehandlingAvsluttet(sobService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "22");

        oppdaterSakOgBehandlingAvsluttet.utfør(prosessinstans);

        verify(sobService).sakOgBehandlingAvsluttet(eq("123"), eq(1L), eq("22"));
    }
}