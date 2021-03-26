package no.nav.melosys.service.behandling;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingEventListenerTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;

    private BehandlingEventListener behandlingEventListener;

    private final long behandlingID = 111;
    private final Behandling behandling = new Behandling();

    @BeforeEach
    public void setup() {
        behandling.setId(123321L);
        behandlingEventListener = new BehandlingEventListener(behandlingService, oppgaveService);
    }

    @Test
    void dokumentBestilt_dokumentErInnvilgelsesbrev_ingenAksjon() throws IkkeFunnetException {
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(behandlingID, Produserbaredokumenter.INNVILGELSE_YRKESAKTIV));
        verifyNoInteractions(behandlingService);
    }

    @Test
    void dokumentBestilt_dokumentErMangelbrevBehandlingIkkeAktiv_ingenAksjon() throws IkkeFunnetException {
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(behandlingID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER));
        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verifyNoMoreInteractions(behandlingService);
    }

    @Test
    void dokumentBestilt_dokumentErMangelbrevBehandlingErAktiv_oppdatererStatusOgFrist() throws IkkeFunnetException {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(behandlingID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER));
        verify(behandlingService).oppdaterStatusOgSvarfrist(eq(behandling), eq(Behandlingsstatus.AVVENT_DOK_PART), any(Instant.class));
    }
}