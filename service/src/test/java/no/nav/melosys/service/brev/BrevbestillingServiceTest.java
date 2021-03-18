package no.nav.melosys.service.brev;

import java.nio.charset.StandardCharsets;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevbestillingServiceTest {

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private DokumentServiceFasade mockDokServiceFasade;

    @Mock
    private DokgenService mockDokgenService;

    private BrevbestillingService brevbestillingService;

    @BeforeEach
    void init() {
        brevbestillingService = new BrevbestillingService(mockBehandlingService, mockDokServiceFasade, mockDokgenService, mock(BrevmottakerService.class), mock(PersondataFasade.class), mock(EregFasade.class), mock(KontaktopplysningService.class));
    }

    @Test
    void gittIkkeAvsluttetBehandlingSkalTilgjengeligeBrevmalerReturneres() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());

        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentBrevMaler(123L);

        assertEquals(2, brevMaler.size());
        assertTrue(brevMaler.containsAll(asList(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER)));
    }

    @Test
    void skalBestilleProduseringAvBrev() throws Exception {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder().build();
        brevbestillingService.produserBrev(MANGELBREV_BRUKER, 123L, brevbestillingDto);

        verify(mockDokgenService).produserOgDistribuerBrev(eq(MANGELBREV_BRUKER), anyLong(), any());
    }

    @Test
    void skalReturnereUtkast() throws Exception {
        byte[] pdf = "UTKAST".getBytes(StandardCharsets.UTF_8);
        when(mockDokServiceFasade.produserUtkast(any(), anyLong(), any())).thenReturn(pdf);
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder().build();

        byte[] utkast = brevbestillingService.produserUtkast(MANGELBREV_BRUKER, 123L, brevbestillingDto);

        assertEquals(pdf, utkast);
        verify(mockDokServiceFasade).produserUtkast(eq(MANGELBREV_BRUKER), anyLong(), any());
    }

}