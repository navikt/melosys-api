package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendForvaltningsmeldingTest {

    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private BehandlingService behandlingService;

    private SendForvaltningsmelding sendForvaltningsmelding;

    @BeforeEach
    public void setUp() {
        sendForvaltningsmelding = new SendForvaltningsmelding(brevBestiller, behandlingService);
    }

    @Test
    void utfør_skalSendesForvaltningsmelding_bestillerForvaltningsmelding() throws TekniskException, FunksjonellException {
        final long behandlingID = 21432L;
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, Boolean.TRUE);
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setId(behandlingID);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");

        sendForvaltningsmelding.utfør(prosessinstans);

        verify(behandlingService).hentBehandling(behandlingID);
        verify(brevBestiller).bestill(eq(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID), anyString(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(Behandling.class));
    }

    @Test
    void utfør_skalIkkeSendeForvaltningsmelding_senderIkke() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        sendForvaltningsmelding.utfør(prosessinstans);
        verify(brevBestiller, never()).bestill(any());
    }
}
