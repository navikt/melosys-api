package no.nav.melosys.service.brev.bestilling;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduserUtkastServiceTest {

    @Mock
    private DokgenService dokgenService;

    @Mock
    private DokumentService dokumentService;

    @InjectMocks
    private ProduserUtkastService produserUtkastService;

    @Test
    void produserUtkast_medTilgjengeligDokgenmal_forventerViBrukerVårDokgen() {
        when(dokgenService.erTilgjengeligDokgenmal(Produserbaredokumenter.FRITEKSTBREV))
            .thenReturn(true);
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(FRITEKSTBREV);


        produserUtkastService.produserUtkast(333L, brevbestillingDto);


        verify(dokgenService).produserUtkast(333L, brevbestillingDto);
        verify(dokumentService, never()).produserUtkast(anyLong(), any());
    }

    @Test
    void produserUtkast_medIngenTilgjengeligDokgenmal_forventerViBrukerDokumentService() {
        when(dokgenService.erTilgjengeligDokgenmal(Produserbaredokumenter.FRITEKSTBREV))
            .thenReturn(false);
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(FRITEKSTBREV);


        produserUtkastService.produserUtkast(333L, brevbestillingDto);


        verify(dokumentService).produserUtkast(333L, brevbestillingDto);
        verify(dokgenService, never()).produserUtkast(anyLong(), any());
    }
}
