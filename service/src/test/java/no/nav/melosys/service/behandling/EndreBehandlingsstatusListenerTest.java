package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.dokument.DokumentBestiltEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndreBehandlingsstatusListenerTest {

    @Mock
    private BehandlingService behandlingService;

    private EndreBehandlingsstatusListener endreBehandlingsstatusListener;

    private final long behandlingID = 111;
    private final Behandling behandling = new Behandling();

    @BeforeEach
    public void setup() {
        endreBehandlingsstatusListener = new EndreBehandlingsstatusListener(behandlingService);
    }

    @Test
    void dokumentBestilt_dokumentErInnvilgelsesbrev_ingenAksjon() throws IkkeFunnetException {
        endreBehandlingsstatusListener.dokumentBestilt(new DokumentBestiltEvent(behandlingID, Produserbaredokumenter.INNVILGELSE_YRKESAKTIV));
        verifyNoInteractions(behandlingService);
    }

    @Test
    void dokumentBestilt_dokumentErMangelbrevBehandlingIkkeAktiv_ingenAksjon() throws IkkeFunnetException {
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        endreBehandlingsstatusListener.dokumentBestilt(new DokumentBestiltEvent(behandlingID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER));
        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verifyNoMoreInteractions(behandlingService);
    }

    @Test
    void dokumentBestilt_dokumentErMangelbrevBehandlingErAktiv_oppdatererStatusOgFrist() throws IkkeFunnetException {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        endreBehandlingsstatusListener.dokumentBestilt(new DokumentBestiltEvent(behandlingID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER));
        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.AVVENT_DOK_PART);
        assertThat(LocalDate.ofInstant(behandling.getDokumentasjonSvarfristDato(), ZoneId.systemDefault()))
            .isEqualTo(LocalDate.ofInstant(Instant.now().plus(Period.ofWeeks(4)), ZoneId.systemDefault()));
        verify(behandlingService).lagre(any(Behandling.class));
    }
}