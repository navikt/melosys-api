package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendSvarAnmodningUnntakTest {

    @Mock
    private EessiService eessiService;

    private SendSvarAnmodningUnntak sendSvarAnmodningUnntak;

    @Before
    public void setup() {
        sendSvarAnmodningUnntak = new SendSvarAnmodningUnntak(eessiService);
    }

    @Test
    public void utfør() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        sendSvarAnmodningUnntak.utfør(prosessinstans);

        verify(eessiService).sendAnmodningUnntakSvar(anyLong());
    }
}