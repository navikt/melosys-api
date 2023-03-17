package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.UtkastBrevRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtkastBrevServiceTest {

    private final long BEHANDLING_ID = 300L;
    private final String DOKUMENT_TITTEL = "Dokumenttittel";

    @Mock
    private UtkastBrevRepository utkastBrevRepository;

    @InjectMocks
    private UtkastBrevService utkastBrevService;

    @Test
    void lagreUtkast_finnesIngenUtkastForSammeBrev_ok() {
        when(utkastBrevRepository.findAllByBehandlingIDOrderByLagringsdatoDesc(BEHANDLING_ID)).thenReturn(List.of(lagUtkastBrev(null)));

        var brevbestillingUtkast = lagBrevbestillingUtkast(DOKUMENT_TITTEL);
        utkastBrevService.lagreUtkast(BEHANDLING_ID, "Z123456", brevbestillingUtkast);

        verify(utkastBrevRepository).save(any());
    }


    @Test
    void lagreUtkast_finnesUtkastForSammeBrev_feiler() {
        when(utkastBrevRepository.findAllByBehandlingIDOrderByLagringsdatoDesc(BEHANDLING_ID)).thenReturn(List.of(lagUtkastBrev(DOKUMENT_TITTEL)));

        var brevbestillingUtkast = lagBrevbestillingUtkast(DOKUMENT_TITTEL);
        assertThrows(
            FunksjonellException.class,
            () -> utkastBrevService.lagreUtkast(BEHANDLING_ID, "Z123456", brevbestillingUtkast)
        );

        verify(utkastBrevRepository, never()).save(any());
    }

    private BrevbestillingUtkast lagBrevbestillingUtkast(String dokumentTittel) {
        return new BrevbestillingUtkast(Produserbaredokumenter.FRITEKSTBREV, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, dokumentTittel, null);
    }

    private UtkastBrev lagUtkastBrev(String dokumentTittel) {
        var utkastBrev = new UtkastBrev();
        utkastBrev.setBrevbestillingUtkast(lagBrevbestillingUtkast(dokumentTittel));
        return utkastBrev;
    }
}
