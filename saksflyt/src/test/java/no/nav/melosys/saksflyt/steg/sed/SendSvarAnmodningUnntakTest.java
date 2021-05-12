package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SendSvarAnmodningUnntakTest {

    @Mock
    private EessiService eessiService;

    private SendSvarAnmodningUnntak sendSvarAnmodningUnntak;

    @BeforeEach
    public void setup() {
        sendSvarAnmodningUnntak = new SendSvarAnmodningUnntak(eessiService);
    }

    @Test
    public void utfør() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        sendSvarAnmodningUnntak.utfør(prosessinstans);

        verify(eessiService).sendAnmodningUnntakSvar(anyLong());
    }
}
