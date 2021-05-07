package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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

        verify(eessiService).sendGodkjenningArbeidFlereLand(anyLong(), isNull());
    }

    @Test
    void varsleUtland_sendA012IkkeValgtAvSaksbehandler_forventIngenSedSendt() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, false);

        sendGodkjenningRegistreringUnntak.utfør(prosessinstans);

        verify(eessiService, never()).sendGodkjenningArbeidFlereLand(anyLong(), isNull());
    }

    @Test
    void varsleUtland_utlandIkkeUtpekt_forventIngenSedSendt() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.getBehandling().setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        sendGodkjenningRegistreringUnntak.utfør(prosessinstans);

        verify(eessiService, never()).sendGodkjenningArbeidFlereLand(anyLong(), isNull());
    }

    private static Prosessinstans lagProsessinstans() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }
}
