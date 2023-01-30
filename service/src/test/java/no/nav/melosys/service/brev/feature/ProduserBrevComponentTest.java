package no.nav.melosys.service.brev.feature;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN_2_8;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProduserBrevComponentTest {

    @Mock
    private DokumentServiceFasade dokumentServiceFasade;

    @InjectMocks
    private ProduserBrevComponent produserBrevComponent;

    @Test
    void skalBestilleProduseringAvBrev() {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder().medProduserbardokument(MANGELBREV_BRUKER).build();
        produserBrevComponent.produserBrev(333L, brevbestillingDto);

        verify(dokumentServiceFasade).produserDokument(anyLong(), any(BrevbestillingDto.class));
    }

    @Test
    void produserBrev_InnvilgelseFtrl_skalIkkeTillates() {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder().medProduserbardokument(INNVILGELSE_FOLKETRYGDLOVEN_2_8).build();
        assertThatThrownBy(() -> produserBrevComponent.produserBrev(333L, brevbestillingDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Manuell bestilling av INNVILGELSE_FOLKETRYGDLOVEN_2_8 er ikke støttet.");
    }
}
