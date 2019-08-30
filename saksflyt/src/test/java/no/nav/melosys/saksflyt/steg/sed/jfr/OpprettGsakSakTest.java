package no.nav.melosys.saksflyt.steg.sed.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.service.sak.FagsakService;
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
public class OpprettGsakSakTest {

    @Mock
    private GsakFasade gsakFasade;
    @Mock
    private FagsakService fagsakService;

    private OpprettGsakSak opprettGsakSak;

    @Before
    public void setup() {
        opprettGsakSak = new OpprettGsakSak(gsakFasade, fagsakService);
    }

    @Test
    public void utfør() throws MelosysException {

        String saksnummer = "saksnr";
        Behandlingstyper behandlingstype = Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD;
        String aktørID = "aktør";
        Long gsakSaksnummer = 12233L;

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);

        Behandling behandling = new Behandling();
        behandling.setType(behandlingstype);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);

        when(gsakFasade.opprettSak(any(), any(), any())).thenReturn(gsakSaksnummer);
        opprettGsakSak.utfør(prosessinstans);

        verify(gsakFasade).opprettSak(
            eq(saksnummer),
            eq(behandlingstype),
            eq(aktørID)
        );

        verify(fagsakService).lagre(eq(fagsak));
        assertThat(prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class)).isEqualTo(gsakSaksnummer);
    }

}