package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerA001;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerA1;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerVedlegg;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrevDataByggerVedleggTest {

    private BrevDataByggerA1 brevDatabyggerA1;
    private BrevDataByggerA001 brevDatabyggerA001;

    private BrevDataA1 brevDataA1;
    private BrevDataA001 brevDataA001;

    public BrevDataByggerVedleggTest() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        brevDatabyggerA1 = mock(BrevDataByggerA1.class);
        brevDatabyggerA001 = mock(BrevDataByggerA001.class);

        brevDataA1 = new BrevDataA1();
        brevDataA001 = new BrevDataA001();

        when(brevDatabyggerA1.lag(any(), any())).thenReturn(brevDataA1);
        when(brevDatabyggerA001.lag(any(), any())).thenReturn(brevDataA001);
    }

    @Test
    public void testByggA1() throws FunksjonellException, TekniskException {
        Behandling behandling = mock(Behandling.class);

        BrevDataBygger brevDataByggerVedlegg = new BrevDataByggerVedlegg(brevDatabyggerA1, null);
        BrevDataVedlegg brevData = (BrevDataVedlegg) brevDataByggerVedlegg.lag(behandling, "Z123456");
        assertThat(brevData.brevDataA1).isEqualTo(brevDataA1);
    }

    @Test
    public void testByggA001() throws FunksjonellException, TekniskException {
        Behandling behandling = mock(Behandling.class);

        BrevDataBygger brevDataByggerVedlegg = new BrevDataByggerVedlegg(brevDatabyggerA001, null);
        BrevDataVedlegg brevData = (BrevDataVedlegg) brevDataByggerVedlegg.lag(behandling, "Z123456");
        assertThat(brevData.brevDataA001).isEqualTo(brevDataA001);
    }

    @Test
    public void testByggA1FraForhåndsvisning() throws FunksjonellException, TekniskException {
        Behandling behandling = mock(Behandling.class);

        BrevbestillingDto brevbestilling = new BrevbestillingDto();
        brevbestilling.mottaker = Aktoersroller.BRUKER;
        brevbestilling.fritekst = "FRITEKST";
        brevbestilling.begrunnelseKode = "tom";

        BrevDataBygger brevDataByggerVedlegg = new BrevDataByggerVedlegg(brevDatabyggerA001, brevbestilling);
        BrevDataVedlegg brevData = (BrevDataVedlegg) brevDataByggerVedlegg.lag(behandling, "Z123456");
        assertThat(brevData.mottakerRolle).isEqualTo(brevbestilling.mottaker);
        assertThat(brevData).isEqualToComparingOnlyGivenFields(brevbestilling, "begrunnelseKode", "fritekst");
    }
}
