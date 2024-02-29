package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrevDataByggerVedleggTest {

    private BrevDataByggerA1 brevDatabyggerA1;
    private BrevDataByggerA001 brevDatabyggerA001;

    private BrevDataA1 brevDataA1;
    private BrevDataA001 brevDataA001;

    @BeforeEach
    public void setup() {
        brevDatabyggerA1 = mock(BrevDataByggerA1.class);
        brevDatabyggerA001 = mock(BrevDataByggerA001.class);

        brevDataA1 = new BrevDataA1();
        brevDataA001 = new BrevDataA001();

        when(brevDatabyggerA1.lag(any(), any())).thenReturn(brevDataA1);
        when(brevDatabyggerA001.lag(any(), any())).thenReturn(brevDataA001);
    }

    @Test
    void testByggA1() {
        BrevDataBygger brevDataByggerVedlegg = new BrevDataByggerVedlegg(brevDatabyggerA1, null);
        BrevDataVedlegg brevData = (BrevDataVedlegg) brevDataByggerVedlegg.lag(mock(BrevDataGrunnlag.class), "Z123456");
        assertThat(brevData.getBrevDataA1()).isNotNull().isEqualTo(brevDataA1);
    }

    @Test
    void testByggA001() {
        BrevDataBygger brevDataByggerVedlegg = new BrevDataByggerVedlegg(brevDatabyggerA001, null);
        BrevDataVedlegg brevData = (BrevDataVedlegg) brevDataByggerVedlegg.lag(mock(BrevDataGrunnlag.class), "Z123456");
        assertThat(brevData.getBrevDataA001()).isNotNull().isEqualTo(brevDataA001);
    }

    @Test
    void testByggA1FraForhåndsvisning() {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setFritekst("FRITEKST");
        brevbestillingDto.setBegrunnelseKode("tom");

        BrevDataBygger brevDataByggerVedlegg = new BrevDataByggerVedlegg(brevDatabyggerA001, brevbestillingDto);
        BrevDataVedlegg brevData = (BrevDataVedlegg) brevDataByggerVedlegg.lag(mock(BrevDataGrunnlag.class), "Z123456");
        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestillingDto, "begrunnelseKode", "fritekst");
    }
}
