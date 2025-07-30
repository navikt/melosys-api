package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendGodkjenningRegistreringUnntakTest {

    private static final String FRITEKST = "Fritekst her";

    @Mock
    private EessiService eessiService;

    private SendGodkjenningRegistreringUnntak sendGodkjenningRegistreringUnntak;

    @BeforeEach
    public void setUp() {
        sendGodkjenningRegistreringUnntak = new SendGodkjenningRegistreringUnntak(eessiService);
    }

    @Test
    void varsleUtland_skalVarslesOgRettBehandlingstema_forventSedSendt() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, true);

        sendGodkjenningRegistreringUnntak.utfør(prosessinstans);

        verify(eessiService).sendGodkjenningArbeidFlereLand(anyLong(), eq(FRITEKST));
    }

    @Test
    void varsleUtland_sendA012IkkeValgtAvSaksbehandler_forventIngenSedSendt() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, false);

        sendGodkjenningRegistreringUnntak.utfør(prosessinstans);

        verify(eessiService, never()).sendGodkjenningArbeidFlereLand(anyLong(), eq(FRITEKST));
    }

    @Test
    void varsleUtland_utlandIkkeUtpekt_forventIngenSedSendt() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.getBehandling().setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        sendGodkjenningRegistreringUnntak.utfør(prosessinstans);

        verify(eessiService, never()).sendGodkjenningArbeidFlereLand(anyLong(), eq(FRITEKST));
    }

    private static Prosessinstans lagProsessinstans() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND)
            .build();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.YTTERLIGERE_INFO_SED, FRITEKST);

        return prosessinstans;
    }
}
