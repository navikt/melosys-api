package no.nav.melosys.saksflyt.steg.brev;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void utfør_skalSendesForvaltningsmelding_bestillerForvaltningsmelding() {
        final long behandlingID = 21432L;
        var behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setId(behandlingID);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");
        ArgumentCaptor<DoksysBrevbestilling> captor = ArgumentCaptor.forClass(DoksysBrevbestilling.class);


        sendForvaltningsmelding.utfør(prosessinstans);


        verify(behandlingService).hentBehandlingMedSaksopplysninger(behandlingID);
        verify(brevBestiller).bestill(captor.capture());
        assertThat(captor.getValue())
            .extracting(
                Brevbestilling::getProduserbartdokument,
                Brevbestilling::getBehandling,
                Brevbestilling::getAvsenderID,
                DoksysBrevbestilling::getMottakere)
            .containsExactly(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID,
                behandling,
                "TEST",
                List.of(Mottaker.av(Aktoersroller.BRUKER))
            );
    }

    @Test
    void utfør_skalIkkeSendeForvaltningsmelding_senderIkke() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        sendForvaltningsmelding.utfør(prosessinstans);
        verify(brevBestiller, never()).bestill(any());
    }
}
