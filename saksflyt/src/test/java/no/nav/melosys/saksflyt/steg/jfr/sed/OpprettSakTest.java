package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.SakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSakTest {

    @Mock
    private SakService sakService;
    @Mock
    private FagsakService fagsakService;

    private OpprettSak opprettSak;

    @Before
    public void setup() {
        opprettSak = new OpprettSak(fagsakService, sakService);
    }

    @Test
    public void utfør() throws MelosysException {

        String saksnummer = "saksnr";
        Behandlingstema behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER;
        String aktørID = "aktør";
        Long gsakSaksnummer = 12233L;

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setTema(behandlingstema);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        Fagsak fagsakIDb = new Fagsak();
        fagsakIDb.setSaksnummer(saksnummer);
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsakIDb);
        when(sakService.opprettSak(any(), any(), any())).thenReturn(gsakSaksnummer);
        opprettSak.utfør(prosessinstans);

        verify(sakService).opprettSak(
            eq(saksnummer),
            eq(behandlingstema),
            eq(aktørID)
        );

        assertThat(fagsakIDb.getGsakSaksnummer()).isEqualTo(gsakSaksnummer);
        verify(fagsakService).lagre(eq(fagsakIDb));
        assertThat(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class)).isEqualTo(gsakSaksnummer);
    }

}