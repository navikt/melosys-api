package no.nav.melosys.service.brev;

import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.UtkastBrevRepository;
import no.nav.melosys.service.brev.bestilling.OppdaterUtkastService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OppdaterUtkastServiceTest {

    private final long UTKAST_BREV_ID = 12L;

    @Mock
    private UtkastBrevRepository utkastBrevRepository;

    @Captor
    private ArgumentCaptor<UtkastBrev> utkastBrevCaptor;

    @InjectMocks
    private OppdaterUtkastService oppdaterUtkastService;

    @Test
    void oppdaterUtkast_mappesRiktig() {
        when(utkastBrevRepository.existsById(UTKAST_BREV_ID)).thenReturn(true);
        OppdaterUtkastService.RequestDto request = lagRequest();


        oppdaterUtkastService.oppdaterUtkast(request);


        verify(utkastBrevRepository).save(utkastBrevCaptor.capture());

        var actual = utkastBrevCaptor.getValue();
        assertEquals(request.utkastBrevID(), actual.getId());
        assertEquals(request.behandlingID(), actual.getBehandlingID());
        assertEquals(request.saksbehandlerIdent(), actual.getLagretAvSaksbehandler());
        assertEquals(request.brevbestillingUtkast(), actual.getBrevbestillingUtkast());
    }

    @Test
    void oppdaterUtkast_utkastFinnes_oppdatererUtkast() {
        when(utkastBrevRepository.existsById(UTKAST_BREV_ID)).thenReturn(true);

        oppdaterUtkastService.oppdaterUtkast(lagRequest());

        verify(utkastBrevRepository).save(any());
    }

    @Test
    void oppdaterUtkast_utkastFinnesIkke_kasterFeil() {
        when(utkastBrevRepository.existsById(UTKAST_BREV_ID)).thenReturn(false);

        assertThrows(FunksjonellException.class, () -> oppdaterUtkastService.oppdaterUtkast(lagRequest()));

        verify(utkastBrevRepository, never()).save(any());
    }

    private OppdaterUtkastService.RequestDto lagRequest() {
        return new OppdaterUtkastService.RequestDto(
            UTKAST_BREV_ID,
            1L,
            "Z123123",
            new BrevbestillingUtkast(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, null, null)
        );
    }
}
