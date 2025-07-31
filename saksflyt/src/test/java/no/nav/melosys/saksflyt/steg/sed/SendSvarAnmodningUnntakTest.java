package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
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

    private final static Long BEHANLING_ID = 1L;
    private final static String YTTERLIGERE_INFO = "Fritekst her";

    @Test
    public void utfør() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANLING_ID)
            .build();
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, YTTERLIGERE_INFO);

        sendSvarAnmodningUnntak.utfør(prosessinstans);

        verify(eessiService).sendAnmodningUnntakSvar(eq(BEHANLING_ID), eq(YTTERLIGERE_INFO));
    }
}
