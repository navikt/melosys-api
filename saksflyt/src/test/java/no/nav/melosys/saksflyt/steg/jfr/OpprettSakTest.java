package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.SakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSakTest {

    @Mock
    private SakService sakService;

    @Mock
    private FagsakService fagsakService;

    private OpprettSak agent;

    @Before
    public void setUp() {
        agent = new OpprettSak(fagsakService, sakService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Behandlingstema behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER;
        String saksnummer = "MEL-009";
        String aktørID = "1000104568393";

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        when(sakService.opprettSak(anyString(), eq(behandlingstema), anyString())).thenReturn(123L);

        Fagsak fagsak = new Fagsak();
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);

        agent.utfør(prosessinstans);

        verify(sakService, times(1)).opprettSak(saksnummer, behandlingstema, aktørID);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.STATUS_BEH_OPPR);
        assertThat(fagsak.getGsakSaksnummer()).isNotNull();
    }
}